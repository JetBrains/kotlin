import java.lang.RuntimeException

val v: String? = param[0]

if (v == null) System.out.println("nullable") else RuntimeException("non nullable projection")