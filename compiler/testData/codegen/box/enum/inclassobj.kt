// IGNORE_BACKEND_FIR: JVM_IR
fun box() = if(Context.operatingSystemType == Context.Companion.OsType.OTHER) "OK" else "fail"

public class Context
{
        companion object
        {
                public enum class OsType {
                        LINUX,
                        OTHER;
                }

                public val operatingSystemType: OsType
                        get() = OsType.OTHER
        }
}
