// "Create type parameter in class 'X'" "false"
// ERROR: 2 type arguments expected for class X<T, U> defined in root package
class X<T, U>
fun Y(x: X<<caret>String>) {}