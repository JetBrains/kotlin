fun box() = if(Context.operatingSystemType == Context.OsType.OTHER) "OK" else "fail"

public class Context
{
        class object
        {
                public enum class OsType {
                        LINUX;
                        OTHER
                }

                public val operatingSystemType: OsType
                        get() = OsType.OTHER
        }
}
