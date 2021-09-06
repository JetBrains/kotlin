// FILE: some/Nls.java
package some
public @interface Nls {
    Capitalization capitalization() default Capitalization.NotSpecified;

    enum Capitalization { NotSpecified, Title, Sentence }
}


// FILE: jvm.kt
import some.Nls.Capitalization.*
import some.Nls

@Nls(<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>Title<!>)
fun f() {

}
