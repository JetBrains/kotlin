/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker.FirTags.MAX_LINE_LENGTH
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker.FirTags.TAG_PREFIX
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker.FirTags.TAG_SUFFIX
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.FirIdenticalCheckerHelper.Companion.isTeamCityBuild
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.latestLVTestDataFile
import org.jetbrains.kotlin.test.utils.llFirTestDataFile
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import org.jetbrains.kotlin.test.utils.reversedTestDataFile
import java.io.File

class TagsGeneratorChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(failedAssertions: List<WrappedException>) {
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val existingTestFiles = listOf(
            originalFile.originalTestDataFile,
            originalFile.firTestDataFile,
            originalFile.llFirTestDataFile,
            originalFile.reversedTestDataFile,
            originalFile.latestLVTestDataFile
        ).filter { it.exists() }

        val firFiles = testServices.moduleStructure.modules.flatMap {
            testServices.artifactsProvider.getArtifactSafe(
                module = it, kind = FrontendKinds.FIR
            )?.allFirFiles?.values.orEmpty()
        }
        addTagsToTestDataFiles(existingTestFiles, firFiles)
    }

    fun addTagsToTestDataFiles(
        testDataFiles: List<File>,
        firFiles: List<FirFile>,
    ) {
        if (!isTeamCityBuild) {
            for (testDataFile in testDataFiles) {
                val testDataFileContent = testDataFile.readText()
                if (testDataFileContent.contains(TAG_PREFIX)) return
                if (firFiles.isEmpty()) return

                val tags = createListOfTags(firFiles)
                val wrappedTagComment = formatTagsAsMultilineComment(tags)

                testDataFile.writer().use {
                    it.append(testDataFileContent.trim())
                    it.append("\n\n")
                    it.appendLine(wrappedTagComment)
                }
            }
        }
    }

    @OptIn(DirectDeclarationsAccess::class)
    fun createListOfTags(firFiles: List<FirFile>): List<String> {
        val visitor = TagsCollectorVisitor(session = firFiles.first().moduleData.session)

        for (file in firFiles) {
            file.accept(visitor)
        }

        return visitor.tags.sorted()
    }

    fun formatTagsAsMultilineComment(tags: List<String>): String {
        val result = StringBuilder(TAG_PREFIX)
        var lineLength = TAG_PREFIX.length

        for (tag in tags) {
            val toAppend = if (result.endsWith(": ") || result.endsWith("\n")) tag else ", $tag"
            if (lineLength + toAppend.length > MAX_LINE_LENGTH) {
                result.appendLine(",")
                result.append(tag)
                lineLength = tag.length
            } else {
                result.append(toAppend)
                lineLength += toAppend.length
            }
        }

        result.append(TAG_SUFFIX)
        return result.toString()
    }

    object FirTags {
        const val TAG_PREFIX = "/* GENERATED_FIR_TAGS: "
        const val TAG_SUFFIX = " */"
        const val MAX_LINE_LENGTH = 120
        const val FUNCTION = "functionDeclaration"
        const val PROPERTY = "propertyDeclaration"
        const val CLASS = "classDeclaration"
        const val TYPEALIAS = "typeAliasDeclaration"
        const val OBJECT = "objectDeclaration"
        const val INTERFACE = "interfaceDeclaration"
        const val ANNOTATION_CLASS = "annotationDeclaration"
        const val ENUM_CLASS = "enumDeclaration"
        const val ENUM_ENTRY = "enumEntry"
        const val SEALED = "sealed"
        const val EXPECT = "expect"
        const val ACTUAL = "actual"
        const val VALUE = "value"
        const val INNER = "inner"
        const val DATA = "data"
        const val TAILREC = "tailrec"
        const val OPERATOR = "operator"
        const val INFIX = "infix"
        const val INLINE = "inline"
        const val EXTERNAL = "external"
        const val SUSPEND = "suspend"
        const val CONST = "const"
        const val LATEINIT = "lateinit"
        const val OVERRIDE = "override"
        const val COMPANION = "companionObject"
        const val VARARG = "vararg"
        const val NOINLINE = "noinline"
        const val CROSSINLINE = "crossinline"
        const val REIFIED = "reified"
        const val OUT = "out"
        const val IN = "in"
        const val PRIMARY_CONSTRUCTOR = "primaryConstructor"
        const val SECONDARY_CONSTRUCTOR = "secondaryConstructor"
        const val INIT_BLOCK = "init"
        const val PROPERTY_DELEGATE = "propertyDelegate"
        const val DESTRUCTURING_DECLARATION = "destructuringDeclaration"
        const val INHERITANCE_DELEGATION = "inheritanceDelegation"
        const val FUN_WITH_EXTENSION_RECEIVER = "funWithExtensionReceiver"
        const val PROPERTY_WITH_EXTENSION_RECEIVER = "propertyWithExtensionReceiver"
        const val IN_PROJECTION = "inProjection"
        const val OUT_PROJECTION = "outProjection"
        const val STAR_PROJECTION = "starProjection"
        const val GETTER = "getter"
        const val SETTER = "setter"
        const val FUNCTIONAL_TYPE = "functionalType"
        const val NULLABLE_TYPE = "nullableType"
        const val DEFINITELY_NOT_NULL = "definitelyNotNullType"
        const val WHILE_LOOP = "whileLoop"
        const val DO_WHILE_LOOP = "doWhileLoop"
        const val FOR_LOOP = "forLoop"
        const val BREAK = "break"
        const val CONTINUE = "continue"
        const val ASSIGNMENT = "assignment"
        const val TYPE_PARAMETER = "typeParameter"
        const val TYPE_CONSTRAINT = "typeConstraint"
        const val COMPARISON = "comparisonExpression"
        const val ANONYMOUS_OBJECT = "anonymousObjectExpression"
        const val ANONYMOUS_FUNCTION = "anonymousFunction"
        const val LAMBDA_LITERAL = "lambdaLiteral"
        const val ANNOTATION_USE_SITE_TARGET_FIELD = "annotationUseSiteTargetField"
        const val ANNOTATION_USE_SITE_TARGET_ALL = "annotationUseSiteTargetAll"
        const val ANNOTATION_USE_SITE_TARGET_FILE = "annotationUseSiteTargetFile"
        const val ANNOTATION_USE_SITE_TARGET_PROPERTY = "annotationUseSiteTargetProperty"
        const val ANNOTATION_USE_SITE_TARGET_PROPERTY_GETTER = "annotationUseSiteTargetPropertyGetter"
        const val ANNOTATION_USE_SITE_TARGET_PROPERTY_SETTER = "annotationUseSiteTargetPropertySetter"
        const val ANNOTATION_USE_SITE_TARGET_SETTER_PARAMETER = "annotationUseSiteTargetSetterParameter"
        const val ANNOTATION_USE_SITE_TARGET_RECEIVER = "annotationUseSiteTargetReceiver"
        const val ANNOTATION_USE_SITE_TARGET_PARAM = "annotationUseSiteTargetParam"
        const val ANNOTATION_USE_SITE_TARGET_FIELD_DELEGATE = "annotationUseSiteTargetFieldDelegate"
        const val TRY_EXPRESSION = "tryExpression"
        const val ELVIS_EXPRESSION = "elvisExpression"
        const val SUPER_EXPRESSION = "superExpression"
        const val THIS_EXPRESSION = "thisExpression"
        const val WHEN_EXPRESSION = "whenExpression"
        const val CONJUNCTION_EXPRESSION = "conjunctionExpression"
        const val DISJUNCTION_EXPRESSION = "disjunctionExpression"
        const val EQUALITY_EXPRESSION = "equalityExpression"
        const val RANGE_EXPRESSION = "rangeExpression"
        const val PROGRESSION_EXPRESSION = "progressionExpression"
        const val AS_EXPRESSION = "asExpression"
        const val IS_EXPRESSION = "isExpression"
        const val STRING_LITERAL = "stringLiteral"
        const val MULTILINE_STRING_LITERAL = "multilineStringLiteral"
        const val GUARD_CONDITION = "guardCondition"
        const val CALLABLE_REFERENCE = "callableReference"
        const val COLLECTION_LITERAL = "collectionLiteral"
        const val IF_EXPRESSION = "ifExpression"
        const val FUNCTION_WITH_CONTEXT = "functionDeclarationWithContext"
        const val PROPERTY_WITH_CONTEXT = "propertyDeclarationWithContext"
        const val FUNCTIONAL_TYPE_WITH_CONTEXT = "typeWithContext"
        const val FUNCTIONAL_TYPE_WITH_EXTENSION = "typeWithExtension"
        const val FUN_INTERFACE = "funInterface"
        const val SAM_CONVERSION = "samConversion"
        const val SMARTCAST = "smartcast"
        const val SAFE_CALL = "safeCall"
        const val LOCAL_CLASS = "localClass"
        const val LOCAL_FUNCTION = "localFunction"
        const val LOCAL_PROPERTY = "localProperty"
        const val FLEXIBLE_TYPE = "flexibleType"
        const val CAPTURED_TYPE = "capturedType"
        const val INTERSECTION_TYPE = "intersectionType"
        const val WHEN_WITH_SUBJECT = "whenWithSubject"
        const val UNNAMED_LOCAL_VARIABLE = "unnamedLocalVariable"
        const val CLASS_REFERENCE = "classReference"
        const val ADDITIVE_EXPRESSION = "additiveExpression"
        const val MULTIPLICATIVE_EXPRESSION = "multiplicativeExpression"
        const val UNARY_EXPRESSION = "unaryExpression"
        const val INCREMENT_DECREMENT_EXPRESSION = "incrementDecrementExpression"
        const val JAVA_FUNCTION = "javaFunction"
        const val JAVA_PROPERTY = "javaProperty"
        const val JAVA_TYPE = "javaType"
        const val CHECK_NOT_NULL_CALL = "checkNotNullCall"
        const val NESTED_CLASS = "nestedClass"
    }
}