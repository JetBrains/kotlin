// A less-than sign at the start of a function name is reported, since it is not supported by the KLIB format
fun `<function`() {}

// A less-than sign elsewhere in the name is not reported
fun `my<function`() {}
