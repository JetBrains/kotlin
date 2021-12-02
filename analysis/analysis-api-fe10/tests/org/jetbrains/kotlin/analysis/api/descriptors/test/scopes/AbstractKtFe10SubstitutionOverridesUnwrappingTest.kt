package org.jetbrains.kotlin.analysis.api.descriptors.test.scopes

import org.jetbrains.kotlin.analysis.api.descriptors.test.KtFe10FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.base.test.scopes.AbstractSubstitutionOverridesUnwrappingTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.symbols.SymbolTestDirectives
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractKtFe10SubstitutionOverridesUnwrappingTest :
    AbstractSubstitutionOverridesUnwrappingTest(KtFe10FrontendApiTestConfiguratorService) {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                // TODO: remove this to enable checking symbol restoration when FE1.0 symbols can be restored correctly
                +SymbolTestDirectives.DO_NOT_CHECK_SYMBOL_RESTORE
            }
        }
    }
}
