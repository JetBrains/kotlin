package org.jetbrains.kotlin.jklib.test.runners

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.additionalK2ConfigurationForIrTextTest
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*

abstract class AbstractFirJKlibIrTextTest(
    val parser: FirParser
) : AbstractJKlibIrTextTest<FirOutputArtifact>(FrontendKinds.FIR) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliJKlibFacade
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliJKlibFacade
    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::BackendCliJKlibFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.additionalK2ConfigurationForIrTextTest(parser)
    }
}
