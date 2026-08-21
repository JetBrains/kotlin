/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.rendering.KaClassTypeQualification
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.rendering.KaTextAttribute
import org.jetbrains.kotlin.analysis.api.rendering.append
import org.jetbrains.kotlin.analysis.api.rendering.render
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.analysis.api.symbols.findPackage
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder

internal fun KaRendererBuilder.pushNameRenderers() {
    push(ClassNameRenderer)
    push(PackageRenderer)
    push(PackageNameRenderer)
}

private object ClassNameRenderer : KaPieceRenderer<ClassId>(KaPiece.ClassName) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: ClassId, next: () -> Unit): Boolean {
        // Reconstructing and resolving the class ID of each segment is only needed to link the segments to their classifiers.
        if (!context.valueFor(KaRenderingOption.LinkSymbols)) {
            renderQualifiedClassName(value.packageFqName, value.relativeClassName.pathSegments()) { segmentName ->
                output.append(segmentName.render(), KaTextAttribute.Identifier)
            }
            return true
        }

        // The class ID which each segment names, so that every segment can be linked to the classifier it refers to.
        val segments = buildList {
            var segmentClassId: ClassId? = null
            for (segmentName in value.relativeClassName.pathSegments()) {
                segmentClassId = segmentClassId?.createNestedClassId(segmentName) ?: ClassId(value.packageFqName, segmentName)
                add(segmentName to segmentClassId)
            }
        }

        renderQualifiedClassName(value.packageFqName, segments) { [segmentName, segmentClassId] ->
            val symbol = findClass(segmentClassId)
            if (symbol != null) {
                identifier(segmentName, symbol)
            } else {
                output.append(segmentName.render(), KaTextAttribute.Identifier)
            }
        }
        return true
    }
}

private object PackageRenderer : KaPieceRenderer<KaPackageSymbol>(KaPiece.Package) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPackageSymbol, next: () -> Unit): Boolean {
        keyword(KtTokens.PACKAGE_KEYWORD)
        render(value, KaPiece.SymbolName)
        return true
    }
}

private object PackageNameRenderer : KaPieceRenderer<FqName>(KaPiece.PackageName) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: FqName, next: () -> Unit): Boolean {
        // Resolving the package which each segment forms is only needed to link the segments to their packages.
        val linkSymbols = context.valueFor(KaRenderingOption.LinkSymbols)
        var current = FqName.ROOT
        value.pathSegments().forEachIndexed { index, segment ->
            if (index > 0) output.append(".", KaTextAttribute.Punctuation)
            val packageSymbol = if (linkSymbols) {
                current = current.child(segment)
                findPackage(current)
            } else {
                null
            }
            if (packageSymbol != null) {
                identifier(segment, packageSymbol)
            } else {
                output.append(segment.render(), KaTextAttribute.Identifier)
            }
        }
        return true
    }
}

/**
 * Renders a qualified class name as prescribed by [KaRenderingOption.ClassTypeQualification]: the package name prefix when the name is
 * fully qualified, followed by the dot-separated [segments], each rendered by [renderSegment].
 */
context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
internal fun <T> renderQualifiedClassName(packageFqName: FqName, segments: List<T>, renderSegment: (T) -> Unit) {
    val qualification = context.valueFor(KaRenderingOption.ClassTypeQualification)

    if (qualification == KaClassTypeQualification.FULLY_QUALIFIED) {
        if (!packageFqName.isRoot && packageFqName != CallableId.PACKAGE_FQ_NAME_FOR_LOCAL) {
            render(packageFqName, KaPiece.PackageName)
            output.append(".", KaTextAttribute.Punctuation)
        }
    }

    // `SIMPLE` keeps only the innermost segment; the other modes render all outer classifiers.
    val renderedSegments = when (qualification) {
        KaClassTypeQualification.SIMPLE -> listOf(segments.last())
        else -> segments
    }

    renderedSegments.forEachIndexed { index, segment ->
        if (index > 0) output.append(".", KaTextAttribute.Punctuation)
        renderSegment(segment)
    }
}
