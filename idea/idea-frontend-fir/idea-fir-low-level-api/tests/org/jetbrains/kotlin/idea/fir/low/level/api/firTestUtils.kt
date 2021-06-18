/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

internal fun Project.allModules() = ModuleManager.getInstance(this).modules.toList()


internal fun FirElement.renderWithClassName(renderMode: FirRenderer.RenderMode = FirRenderer.RenderMode.Normal): String =
    "${this::class.simpleName} `${render(renderMode)}`"


internal fun AbstractFirBasedSymbol<*>.name(): String = when (this) {
    is FirCallableSymbol<*> -> callableId.callableName.asString()
    is FirClassLikeSymbol<*> -> classId.shortClassName.asString()
    is FirAnonymousInitializerSymbol -> "<init>"
    else -> error("unknown symbol ${this::class.simpleName}")
}

internal fun FirDeclaration.name(): String = when (this) {
    is FirSymbolOwner<*> -> symbol.name()
    is FirFile -> "<FILE>"
    else -> error("unknown declaration ${this::class.simpleName}")
}