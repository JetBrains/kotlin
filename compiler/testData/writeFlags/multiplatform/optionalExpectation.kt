// !LANGUAGE: +MultiPlatformProjects
// !USE_EXPERIMENTAL: kotlin.ExperimentalMultiplatform
// WITH_RUNTIME

@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE") // TODO: support common sources in the test infrastructure

@OptionalExpectation
expect annotation class Anno(val s: String)

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: Anno
// FLAGS: ACC_INTERFACE, ACC_ABSTRACT, ACC_ANNOTATION
