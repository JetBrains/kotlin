package org.jetbrains.kotlin.resolve.validation

object DefaultSymbolUsageValidator : CompositeSymbolUsageValidator(DeprecatedSymbolValidator())