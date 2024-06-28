// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

expect interface <!NO_ACTUAL_FOR_EXPECT!>Dummy<!> {
    interface ExternalInterface
}

<!WRONG_MODIFIER_TARGET!>external<!> class ExternalClass: Dummy.ExternalInterface

// MODULE: main-js()()(common)

actual external interface Dummy {
    actual interface ExternalInterface
}
