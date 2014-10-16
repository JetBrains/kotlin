// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS

import kotlin.modules.ModuleBuilder

fun foo(x: Int,
        moduleBuilder: ModuleBuilder) {
    foo(, ModuleBuilder("", ""));
    foo(1, ModuleBuilder("", ""));
    foo(2, ModuleBuilder("", ""));
}