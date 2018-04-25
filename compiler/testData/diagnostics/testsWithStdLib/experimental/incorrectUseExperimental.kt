// !API_VERSION: 1.3

<!USE_EXPERIMENTAL_WITHOUT_ARGUMENTS!>@UseExperimental<!>
fun f1() {}

<!USE_EXPERIMENTAL_ARGUMENT_IS_NOT_MARKER!>@UseExperimental(UseExperimental::class)<!>
fun f2() {}
