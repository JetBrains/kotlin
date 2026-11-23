import lib.api.Api
import lib.extra.Extra
import other.api.OtherApi
import other.extra.OtherExtra

fun test(): List<Any> = listOf(Api(), Extra(), OtherApi(), OtherExtra())
