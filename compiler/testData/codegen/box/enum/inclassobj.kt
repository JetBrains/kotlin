fun box() = if(Context.operatingSystemType == Context.Default.OsType.OTHER) "OK" else "fail"

public class Context
{
        default object
        {
                public enum class OsType {
                        LINUX;
                        OTHER
                }

                public val operatingSystemType: OsType
                        get() = OsType.OTHER
        }
}
