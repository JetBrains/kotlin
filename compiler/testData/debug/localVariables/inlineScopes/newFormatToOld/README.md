When compiling the code with inline scopes numbers and then inlining it with the old format, the resulting bytecode won't contain
enough $iv suffixes. This happens because the new format doesn't add $iv suffixes when the functions are inlined, but adds scope numbers
instead. These tests are only meant to test that the compiler doesn't crash in the scenario when the code is first compiled with the new
format and then inlined with the old one.