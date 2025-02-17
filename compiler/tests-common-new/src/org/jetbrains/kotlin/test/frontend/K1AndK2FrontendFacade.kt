/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend

import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runWithEnablingFirUseOption
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager

class K1AndK2FrontendFacade(
    testServices: TestServices,
) : FrontendFacade<K1AndK2OutputArtifact>(testServices, FrontendKinds.ClassicAndFIR) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = classicFrontendFacade.additionalServices + firFrontendFacade.additionalServices

    private val classicFrontendFacade: ClassicFrontendFacade = ClassicFrontendFacade(testServices)
    private val firFrontendFacade: FirFrontendFacade = FirFrontendFacade(testServices)

    override fun analyze(module: TestModule): K1AndK2OutputArtifact {
        testServices.compiledClassesManager.specifiedFrontendKind = FrontendKinds.ClassicFrontend
        val k1Artifact = classicFrontendFacade.analyze(module)
        testServices.compiledClassesManager.specifiedFrontendKind = FrontendKinds.FIR
        val k2Artifact = runWithEnablingFirUseOption(testServices, module) { firFrontendFacade.analyze(module) }
        return K1AndK2OutputArtifact(k1Artifact, k2Artifact)
    }

}