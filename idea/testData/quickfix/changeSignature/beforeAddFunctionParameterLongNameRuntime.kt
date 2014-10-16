// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS

fun foo(x: Int) {
    foo(,);
    foo(1);
    foo(2, kotlin.modules.ModuleBuilder("", "")<caret>);
}