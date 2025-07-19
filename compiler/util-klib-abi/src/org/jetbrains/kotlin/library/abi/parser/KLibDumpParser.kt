/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalLibraryAbiReader::class)

package org.jetbrains.kotlin.library.abi.parser

import org.jetbrains.kotlin.library.abi.AbiClass
import org.jetbrains.kotlin.library.abi.AbiCompoundName
import org.jetbrains.kotlin.library.abi.AbiDeclaration
import org.jetbrains.kotlin.library.abi.AbiEnumEntry
import org.jetbrains.kotlin.library.abi.AbiFunction
import org.jetbrains.kotlin.library.abi.AbiModality
import org.jetbrains.kotlin.library.abi.AbiProperty
import org.jetbrains.kotlin.library.abi.AbiQualifiedName
import org.jetbrains.kotlin.library.abi.AbiSignatureVersion
import org.jetbrains.kotlin.library.abi.AbiSignatures
import org.jetbrains.kotlin.library.abi.AbiValueParameterKind
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbi
import org.jetbrains.kotlin.library.abi.LibraryManifest
import org.jetbrains.kotlin.library.abi.impl.AbiAnnotationListImpl
import org.jetbrains.kotlin.library.abi.impl.AbiClassImpl
import org.jetbrains.kotlin.library.abi.impl.AbiConstructorImpl
import org.jetbrains.kotlin.library.abi.impl.AbiEnumEntryImpl
import org.jetbrains.kotlin.library.abi.impl.AbiFunctionImpl
import org.jetbrains.kotlin.library.abi.impl.AbiPropertyImpl
import org.jetbrains.kotlin.library.abi.impl.AbiSignaturesImpl
import org.jetbrains.kotlin.library.abi.impl.AbiTopLevelDeclarationsImpl
import org.jetbrains.kotlin.library.abi.impl.AbiValueParameterImpl

private class MutableAbiInfo(
    val declarations: MutableList<AbiDeclaration> = mutableListOf(),
    var uniqueName: String = "",
    var signatureVersions: MutableSet<AbiSignatureVersion> = mutableSetOf(),
)

class KlibDumpParser(klibDump: String, private val fileName: String? = null) {

    /** Cursor to keep track of current location within the dump */
    private val cursor = Cursor(klibDump)
    /** The set of targets that the declarations being parsed belong to */
    private val currentTargetNames = mutableSetOf<String>()
    private val currentTargets: List<MutableAbiInfo>
        get() =
            currentTargetNames.map {
                abiInfoByTarget[it]
                    ?: throw IllegalStateException("Expected target $it to exist in map")
            }

    /**
     * Map of all targets to the declarations that belong to them. Only update the
     * [currentTargetNames] when parsing declarations.
     */
    private val abiInfoByTarget = mutableMapOf<String, MutableAbiInfo>()

    /** Parse the klib dump tracked by [cursor] into a map of targets to [LibraryAbi]s */
    fun parse(): Map<String, LibraryAbi> {
        while (!cursor.isFinished()) {
            parseDeclaration(parentQualifiedName = null)?.let { abiDeclaration ->
                // Find all the targets the current declaration belongs to
                currentTargets.forEach {
                    // and add the parsed declaration to the list for those targets
                    it.declarations.add(abiDeclaration)
                }
            }
        }
        if (abiInfoByTarget.isEmpty()) {
            throw ParseException("No targets were found")
        }
        return abiInfoByTarget
            .map { (target, abiInfo) ->
                target to
                        LibraryAbi(
                            uniqueName = abiInfo.uniqueName,
                            signatureVersions = abiInfo.signatureVersions,
                            topLevelDeclarations = AbiTopLevelDeclarationsImpl(abiInfo.declarations),
                            manifest =
                                LibraryManifest(
                                    platform = target,
                                    // To be completed in follow up CLs. This information is currently
                                    // not
                                    // considered when checking for compatibility
                                    platformTargets = listOf(),
                                    compilerVersion = "",
                                    abiVersion = "",
                                    irProviderName = "",
                                ),
                        )
            }
            .toMap()
    }


    internal fun parseClass(parentQualifiedName: AbiQualifiedName? = null): AbiClass {
        val modality =
            cursor.parseAbiModality() ?: throw parseException("Failed to parse class modality")
        val modifiers = cursor.parseClassModifiers()
        val isInner = modifiers.contains("inner")
        val isValue = modifiers.contains("value")
        val isFunction = modifiers.contains("fun")
        val kind = cursor.parseClassKind() ?: throw parseException("Failed to parse class kind")
        val typeParams = cursor.parseTypeParams() ?: emptyList()
        // if we are a nested class the name won't be qualified, and we will need to use the
        // [parentQualifiedName] to complete it
        val abiQualifiedName = parseAbiQualifiedName(parentQualifiedName)
        val superTypes = cursor.parseSuperTypes()

        val childDeclarations =
            if (cursor.parseOpenClassBody() != null) {
                cursor.nextLine()
                parseChildDeclarations(abiQualifiedName)
            } else {
                emptyList()
            }
        return AbiClassImpl(
            qualifiedName = abiQualifiedName,
            signatures = currentSignatures(),
            annotations = AbiAnnotationListImpl.EMPTY, // annotations aren't part of klib dumps
            modality = modality,
            kind = kind,
            isInner = isInner,
            isValue = isValue,
            isFunction = isFunction,
            superTypes = superTypes.toList(),
            declarations = childDeclarations,
            typeParameters = typeParams,
        )
    }

    internal fun parseFunction(
        parentQualifiedName: AbiQualifiedName? = null,
        isGetterOrSetter: Boolean = false,
    ): AbiFunction {
        val modality = cursor.parseAbiModality()
        val isConstructor = cursor.parseFunctionKind(peek = true) == "constructor"
        return when {
            isConstructor -> parseConstructor(parentQualifiedName)
            else ->
                parseNonConstructorFunction(
                    parentQualifiedName,
                    isGetterOrSetter,
                    modality ?: throw parseException("Non constructor function must have modality"),
                )
        }
    }

    internal fun parseProperty(parentQualifiedName: AbiQualifiedName? = null): AbiProperty {
        val modality =
            cursor.parseAbiModality()
                ?: throw parseException("Unable to parse modality for property")
        val kind =
            cursor.parsePropertyKind() ?: throw parseException("Unable to parse kind for property")
        val qualifiedName = parseAbiQualifiedName(parentQualifiedName)

        cursor.nextLine()
        var getter: AbiFunction? = null
        var setter: AbiFunction? = null
        while (cursor.hasGetterOrSetter()) {
            if (cursor.hasGetter()) {
                getter = parseFunction(qualifiedName, isGetterOrSetter = true)
            } else {
                setter = parseFunction(qualifiedName, isGetterOrSetter = true)
            }
            if (cursor.isFinished()) {
                break
            }
        }
        return AbiPropertyImpl(
            qualifiedName = qualifiedName,
            signatures = currentSignatures(),
            annotations = AbiAnnotationListImpl.EMPTY, // annotations aren't part of klib dumps
            modality = modality,
            kind = kind,
            getter = getter,
            setter = setter,
            backingField = null,
        )
    }

    internal fun parseEnumEntry(parentQualifiedName: AbiQualifiedName?): AbiEnumEntry {
        cursor.parseEnumEntryKind()
        val enumName = cursor.parseEnumName() ?: throw parseException("Failed to parse enum name")
        val relativeName =
            parentQualifiedName?.let { it.relativeName.value + "." + enumName }
                ?: throw parseException("Enum entry must have parent qualified name")
        val qualifiedName =
            AbiQualifiedName(parentQualifiedName.packageName, AbiCompoundName(relativeName))
        cursor.nextLine()
        return AbiEnumEntryImpl(
            qualifiedName = qualifiedName,
            signatures = currentSignatures(),
            annotations = AbiAnnotationListImpl.EMPTY,
        )
    }

    private fun parseDeclaration(parentQualifiedName: AbiQualifiedName?): AbiDeclaration? {
        // if the line begins with a comment, we may need to parse the current target list
        if (cursor.parseCommentMarker() != null) {
            parseCommentLine()
        } else if (cursor.hasClassKind()) {
            return parseClass(parentQualifiedName)
        } else if (cursor.hasFunctionKind()) {
            return parseFunction(parentQualifiedName)
        } else if (cursor.hasPropertyKind()) {
            return parseProperty(parentQualifiedName)
        } else if (cursor.hasEnumEntry()) {
            return parseEnumEntry(parentQualifiedName)
        } else if (cursor.currentLine.isBlank()) {
            cursor.nextLine()
        } else {
            throw parseException("Failed to parse unknown declaration")
        }
        return null
    }

    private fun parseCommentLine() {
        if (cursor.hasTargets()) {
            // There are never targets within targets, so when we encounter a new directive we
            // always reset our current targets
            val targets = cursor.parseTargets()
            targets.forEach { abiInfoByTarget.putIfAbsent(it, MutableAbiInfo()) }
            currentTargetNames.clear()
            currentTargetNames.addAll(targets)
        } else if (cursor.hasUniqueName()) {
            val uniqueName =
                cursor.parseUniqueName()
                    ?: throw parseException("Failed to parse library unique name")
            currentTargets.forEach { it.uniqueName = uniqueName }
        } else if (cursor.hasSignatureVersion()) {
            val signatureVersion =
                cursor.parseSignatureVersion()
                    ?: throw parseException("Failed to parse signature version")
            currentTargets.forEach { it.signatureVersions.add(signatureVersion) }
        }
        cursor.nextLine()
    }

    /** Parse all declarations which belong to a parent such as a class */
    private fun parseChildDeclarations(
        parentQualifiedName: AbiQualifiedName?
    ): List<AbiDeclaration> {
        val childDeclarations = mutableListOf<AbiDeclaration>()
        // end of parent container is marked by a closing bracket, collect all declarations
        // until we see one.
        while (cursor.parseCloseClassBody(peek = true) == null) {
            parseDeclaration(parentQualifiedName)?.let { childDeclarations.add(it) }
        }
        cursor.nextLine()
        return childDeclarations
    }

    private fun parseNonConstructorFunction(
        parentQualifiedName: AbiQualifiedName? = null,
        isGetterOrSetter: Boolean = false,
        modality: AbiModality,
    ): AbiFunction {
        val modifiers = cursor.parseFunctionModifiers()
        val isInline = modifiers.contains("inline")
        val isSuspend = modifiers.contains("suspend")
        cursor.parseFunctionKind()
        val typeParams = cursor.parseTypeParams() ?: emptyList()
        val contextParams = cursor.parseContextParams() ?: emptyList()
        val functionReceiver = cursor.parseFunctionReceiver()
        val abiQualifiedName =
            if (isGetterOrSetter) {
                parseAbiQualifiedNameForGetterOrSetter(parentQualifiedName)
            } else {
                parseAbiQualifiedName(parentQualifiedName)
            }
        val valueParameters =
            cursor.parseValueParameters() ?: throw parseException("Couldn't parse value params")
        val allValueParameters =
            contextParams +
                    if (null != functionReceiver) {
                        val functionReceiverAsValueParam =
                            AbiValueParameterImpl(
                                kind = AbiValueParameterKind.EXTENSION_RECEIVER,
                                type = functionReceiver,
                                isVararg = false,
                                hasDefaultArg = false,
                                isNoinline = false,
                                isCrossinline = false,
                            )
                        listOf(functionReceiverAsValueParam) + valueParameters
                    } else {
                        valueParameters
                    }
        val returnType = cursor.parseReturnType()
        cursor.nextLine()
        return AbiFunctionImpl(
            qualifiedName = abiQualifiedName,
            signatures = currentSignatures(),
            annotations = AbiAnnotationListImpl.EMPTY, // annotations aren't part of klib dumps
            modality = modality,
            isInline = isInline,
            isSuspend = isSuspend,
            typeParameters = typeParams,
            valueParameters = allValueParameters,
            returnType = returnType,
        )
    }

    private fun parseConstructor(parentQualifiedName: AbiQualifiedName?): AbiFunction {
        val abiQualifiedName =
            parentQualifiedName?.let {
                AbiQualifiedName(
                    parentQualifiedName.packageName,
                    AbiCompoundName(parentQualifiedName.relativeName.value + ".<init>"),
                )
            } ?: throw parseException("Cannot parse constructor outside of class context")
        cursor.parseConstructorName()
        val valueParameters =
            cursor.parseValueParameters()
                ?: throw parseException("Couldn't parse value parameters for constructor")
        cursor.nextLine()
        return AbiConstructorImpl(
            qualifiedName = abiQualifiedName,
            signatures = currentSignatures(),
            annotations = AbiAnnotationListImpl.EMPTY, // annotations aren't part of klib dumps
            isInline = false, // TODO
            valueParameters = valueParameters,
        )
    }

    private fun parseAbiQualifiedName(parentQualifiedName: AbiQualifiedName?): AbiQualifiedName {
        val hasQualifiedName = cursor.parseAbiQualifiedName(peek = true) != null
        return if (hasQualifiedName) {
            cursor.parseAbiQualifiedName()!!
        } else {
            if (parentQualifiedName == null) {
                throw parseException("Failed to parse qName")
            }
            val identifier = cursor.parseValidIdentifierAndMaybeTrim()
            val relativeName = parentQualifiedName.relativeName.value + "." + identifier
            return AbiQualifiedName(parentQualifiedName.packageName, AbiCompoundName(relativeName))
        }
    }

    private fun parseAbiQualifiedNameForGetterOrSetter(
        parentQualifiedName: AbiQualifiedName?
    ): AbiQualifiedName {
        if (parentQualifiedName == null) {
            throw parseException("Failed to parse qName")
        }
        val identifier =
            cursor.parseGetterOrSetterName() ?: throw parseException("Failed to parse qName")
        val relativeName = parentQualifiedName.relativeName.value + "." + identifier
        return AbiQualifiedName(parentQualifiedName.packageName, AbiCompoundName(relativeName))
    }

    private fun currentSignatures(): AbiSignatures {
        val hasV1 = currentTargets.any { target -> target.signatureVersions.any { it.versionNumber == 1} }
        val hasV2 = currentTargets.any { target -> target.signatureVersions.any { it.versionNumber == 2} }
        return AbiSignaturesImpl(
            if (hasV1) ("v1") else null,
            if (hasV2) ("v2") else null,
        )
    }

    private fun parseException(message: String): ParseException =
        ParseException(formatErrorMessage(message))

    private fun formatErrorMessage(message: String): String {
        val maybeFileName = fileName?.let { "$it:" } ?: ""
        val location = "$maybeFileName${cursor.rowIndex}:${cursor.columnIndex}"
        return "$message at $location: '${cursor.currentLine}'"
    }
}

/** Exception which uses the cursor to include the location of the failure */
class ParseException(message: String) : RuntimeException(message)

