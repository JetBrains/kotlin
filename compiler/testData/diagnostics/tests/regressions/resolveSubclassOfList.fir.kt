import java.util.ArrayList

fun foo(p: java.util.List<String>) {
    p.iterator(); // forcing resolve of java.util.List.iterator()

    ArrayList<String>().iterator(); // this provoked exception in SignaturesPropagationData
}