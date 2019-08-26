// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
package example2

fun box() = Context.OsType.OK.toString()

object Context
{
        public enum class OsType {
                WIN2000, WINDOWS, MACOSX, LINUX, OTHER, OK;
        }
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
