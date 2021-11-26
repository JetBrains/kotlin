// FIR_IDENTICAL
// WITH_EXTENDED_CHECKERS
import java.util.ArrayList

fun foo(p: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.List<String><!>) {
    p.iterator(); // forcing resolve of java.util.List.iterator()

    ArrayList<String>().iterator(); // this provoked exception in SignaturesPropagationData
}