// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS

import kotlin.modules.ModuleBuilder

fun foo(x: Int, moduleBuilder: ModuleBuilder) {
    foo(, kotlin.modules.ModuleBuilder("", ""));
    foo(1, kotlin.modules.ModuleBuilder("", ""));
    foo(2, kotlin.modules.ModuleBuilder("", ""));
}