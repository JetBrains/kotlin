// "Replace Class<T> with KClass<T> in whole annotation" "true"
// ERROR: <html>Type inference failed. Expected type mismatch: <table><tr><td>required: </td><td><b>kotlin.reflect.KClass&lt;*&gt;</b></td></tr><tr><td>found: </td><td><font color=red><b>java.lang.Class&lt;???&gt;</b></font></td></tr></table></html>
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.reflect.KClass&lt;*&gt;</td></tr><tr><td>Found:</td><td>java.lang.Class&lt;[ERROR : Err]&gt;</td></tr></table></html>
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.reflect.KClass&lt;*&gt;</td></tr><tr><td>Found:</td><td>java.lang.Class&lt;kotlin.Double&gt;</td></tr></table></html>
// ERROR: Unresolved reference: Err
// WITH_RUNTIME

annotation class Ann(vararg val arg: Class<*><caret>)

Ann(javaClass<String>(), javaClass<Err>()) class MyClass1
Ann(javaClass<String>(), javaClass()) class MyClass2

val x = javaClass<Double>()
Ann(javaClass<String>(), x) class MyClass3
