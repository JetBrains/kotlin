// MODULE: common
// TARGET_PLATFORM: Common
expect fun foo()

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
actual fun foo() {}

// MODULE: main()()(common, intermediate)
<!AMBIGUOUS_EXPECTS!>actual fun foo() {}<!>
