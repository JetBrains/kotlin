/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.extensions.additionalCheckers
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.registerExtensions

class FirSessionConfigurator(private val session: FirSession) {
    private val registeredExtensions: MutableList<BunchOfRegisteredExtensions> = mutableListOf(BunchOfRegisteredExtensions.empty())

    fun registerExtensions(extensions: BunchOfRegisteredExtensions) {
        registeredExtensions += extensions
    }

    @OptIn(SessionConfiguration::class)
    fun useCheckers(checkers: ExpressionCheckers) {
        session.checkersComponent.register(checkers)
    }

    @OptIn(SessionConfiguration::class)
    fun useCheckers(checkers: DeclarationCheckers) {
        session.checkersComponent.register(checkers)
    }

    @OptIn(SessionConfiguration::class)
    fun useCheckers(checkers: TypeCheckers) {
        session.checkersComponent.register(checkers)
    }

    @SessionConfiguration
    fun configure() {
        session.extensionService.registerExtensions(registeredExtensions.reduce(BunchOfRegisteredExtensions::plus))
        session.extensionService.additionalCheckers.forEach(session.checkersComponent::register)
    }
}