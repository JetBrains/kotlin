// FILE: backend/asmutil/AsmUtil.java
package backend.asmutil;

import org.jetbrains.annotations.NotNull;
import static frontend.JvmDeclarationOrigin.NO_ORIGIN;

public class AsmUtil {

    @NotNull
    public static String doSmth(String s) {
        Object or = NO_ORIGIN;
        return "OK";
    }
}

// FILE: First.kt
package frontend
public class JvmDeclarationOrigin {
    default object {
        public val NO_ORIGIN: JvmDeclarationOrigin = JvmDeclarationOrigin()
    }
}

// FILE: Second.kt
package backend

import backend.asmutil.AsmUtil.doSmth

open public class ECallable  {
    fun test() {
        doSmth("")
    }
}

/* KT-5848 */

