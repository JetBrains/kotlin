// IGNORE_BACKEND_FIR: JVM_IR
package example2

fun box() = Context.OsType.OK.toString()

object Context
{
        public enum class OsType {
                WIN2000, WINDOWS, MACOSX, LINUX, OTHER, OK;
        }
}
