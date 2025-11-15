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

import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.abi.impl.*
import java.io.File
import java.text.ParseException

/**
 * Parser for klib dump format created by [org.jetbrains.kotlin.library.abi.LibraryAbiRenderer]
 *
 * @property klibDump The text of the dump file
 * @property filePath The file path to add to parse exceptions for clearer debugging (optional)
 */
@ExperimentalLibraryAbiReader
class KlibDumpParser(klibDump: String, private val filePath: String? = null) {

    constructor(file: File) : this(file.readText(), file.path)

    /** Cursor to keep track of current location within the dump */
    private val cursor = Cursor(klibDump)
    private val declarations: MutableList<AbiDeclaration> = mutableListOf()
    private var uniqueName: String = ""
    private var signatureVersions: MutableSet<AbiSignatureVersion> = mutableSetOf()

    /**
     * Parse the klib dump text.
     *
     * Returns [LibraryAbi] represented by the dump text.
     *
     * Throws [ParseException] when unable to parse for whatever reason
     **/
    fun parse(): LibraryAbi {
        while (!cursor.isFinished()) {
            parseDeclaration(parentQualifiedName = null)?.let { abiDeclaration ->
                declarations.add(abiDeclaration)
            }
        }
        return LibraryAbi(
            uniqueName = uniqueName,
            signatureVersions = signatureVersions,
            topLevelDeclarations = AbiTopLevelDeclarationsImpl(declarations),
            manifest =
                LibraryManifest(
                    platform = null,
                    platformTargets = emptyList(),
                    compilerVersion = null,
                    abiVersion = null,
                    irProviderName = null,
                ),
        )
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
        while (!cursor.isFinished()) {
            when {
                cursor.hasGetter() ->
                    getter = parseFunction(qualifiedName, isGetterOrSetter = true)
                cursor.hasSetter() ->
                    setter = parseFunction(qualifiedName, isGetterOrSetter = true)
                else -> break
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
        val enumName = cursor.parseEnumEntryName() ?: throw parseException("Failed to parse enum name")
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
        cursor.parseCommentMarker()
        if (cursor.hasLibraryUniqueName()) {
            uniqueName = cursor.parseLibraryUniqueName()
                ?: throw parseException("Failed to parse library unique name")
        } else if (cursor.hasSignatureVersion()) {
            signatureVersions.add(cursor.parseSignatureVersion()
                ?: throw parseException("Failed to parse signature version"))
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
        val contextAndReceiverParams = cursor.parseContextAndReceiverParams()
        val abiQualifiedName =
            if (isGetterOrSetter) {
                parseAbiQualifiedNameForGetterOrSetter(parentQualifiedName)
            } else {
                parseAbiQualifiedName(parentQualifiedName)
            }
        val valueParameters =
            cursor.parseValueParameters() ?: throw parseException("Couldn't parse value params")
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
            valueParameters = contextAndReceiverParams + valueParameters,
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
            isInline = false, // constructors cannot be inline
            valueParameters = valueParameters,
        )
    }

    private fun parseAbiQualifiedName(parentQualifiedName: AbiQualifiedName?): AbiQualifiedName {
        if (parentQualifiedName != null) {
            val identifier = cursor.parseValidIdentifierAndMaybeTrim()
            val relativeName = parentQualifiedName.relativeName.value + "." + identifier
            return AbiQualifiedName(parentQualifiedName.packageName, AbiCompoundName(relativeName))
        }
        return cursor.parseAbiQualifiedName() ?: throw parseException("Failed to parse qName")
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
        val hasV1 = signatureVersions.any { it.versionNumber == 1}
        val hasV2 = signatureVersions.any { it.versionNumber == 2}
        return AbiSignaturesImpl(
            if (hasV1) ("v1") else null,
            if (hasV2) ("v2") else null,
        )
    }

    private fun parseException(message: String): ParseException =
        ParseException(formatErrorMessage(message), cursor.offset)

    private fun formatErrorMessage(message: String): String {
        val maybeFilePath = filePath?.let { "$it:" } ?: ""
        val location = "$maybeFilePath${cursor.rowIndex}:${cursor.columnIndex}"
        return "$message at $location: '${cursor.currentLine}'"
    }
}


