inline class ICString(val a: String)
inline class ICStringNullable(val a: String?)
inline class ICAny(val a: Any)
inline class ICAnyNullable(val a: Any?)
inline class ICInt(val a: Int)
inline class ICIntNullable(val a: Int?)

fun ordinaryNoninlineReturnsICString(): ICString = ICString("")
fun ordinaryNoninlineReturnsICStringNullable(): ICStringNullable = ICStringNullable("")
fun ordinaryNoninlineReturnsICAny(): ICAny = ICAny("")
fun ordinaryNoninlineReturnsICAnyNullable(): ICAnyNullable = ICAnyNullable("")
fun ordinaryNoninlineReturnsICInt(): ICInt = ICInt(0)
fun ordinaryNoninlineReturnsICIntNullable(): ICIntNullable = ICIntNullable(0)
fun ordinaryNoninlineReturnsICString_Null(): ICString? = null
fun ordinaryNoninlineReturnsICStringNullable_Null(): ICStringNullable? = null
fun ordinaryNoninlineReturnsICAny_Null(): ICAny? = null
fun ordinaryNoninlineReturnsICAnyNullable_Null(): ICAnyNullable? = null
fun ordinaryNoninlineReturnsICInt_Null(): ICInt? = null
fun ordinaryNoninlineReturnsICIntNullable_Null(): ICIntNullable? = null
fun ordinaryNoninlineAcceptsICString(i: Int, ic: ICString) {}
fun ordinaryNoninlineAcceptsICStringNullable(i: Int, ic: ICStringNullable) {}
fun ordinaryNoninlineAcceptsICAny(i: Int, ic: ICAny) {}
fun ordinaryNoninlineAcceptsICAnyNullable(i: Int, ic: ICAnyNullable) {}
fun ordinaryNoninlineAcceptsICInt(i: Int, ic: ICInt) {}
fun ordinaryNoninlineAcceptsICIntNullable(i: Int, ic: ICIntNullable) {}
fun ordinaryNoninlineAcceptsICString_Null(i: Int, ic: ICString?) {}
fun ordinaryNoninlineAcceptsICStringNullable_Null(i: Int, ic: ICStringNullable?) {}
fun ordinaryNoninlineAcceptsICAny_Null(i: Int, ic: ICAny?) {}
fun ordinaryNoninlineAcceptsICAnyNullable_Null(i: Int, ic: ICAnyNullable?) {}
fun ordinaryNoninlineAcceptsICInt_Null(i: Int, ic: ICInt?) {}
fun ordinaryNoninlineAcceptsICIntNullable_Null(i: Int, ic: ICIntNullable?) {}
inline fun ordinaryInlineReturnsICString(): ICString = ICString("")
inline fun ordinaryInlineReturnsICStringNullable(): ICStringNullable = ICStringNullable("")
inline fun ordinaryInlineReturnsICAny(): ICAny = ICAny("")
inline fun ordinaryInlineReturnsICAnyNullable(): ICAnyNullable = ICAnyNullable("")
inline fun ordinaryInlineReturnsICInt(): ICInt = ICInt(0)
inline fun ordinaryInlineReturnsICIntNullable(): ICIntNullable = ICIntNullable(0)
inline fun ordinaryInlineReturnsICString_Null(): ICString? = null
inline fun ordinaryInlineReturnsICStringNullable_Null(): ICStringNullable? = null
inline fun ordinaryInlineReturnsICAny_Null(): ICAny? = null
inline fun ordinaryInlineReturnsICAnyNullable_Null(): ICAnyNullable? = null
inline fun ordinaryInlineReturnsICInt_Null(): ICInt? = null
inline fun ordinaryInlineReturnsICIntNullable_Null(): ICIntNullable? = null
inline fun ordinaryInlineAcceptsICString(i: Int, ic: ICString) {}
inline fun ordinaryInlineAcceptsICStringNullable(i: Int, ic: ICStringNullable) {}
inline fun ordinaryInlineAcceptsICAny(i: Int, ic: ICAny) {}
inline fun ordinaryInlineAcceptsICAnyNullable(i: Int, ic: ICAnyNullable) {}
inline fun ordinaryInlineAcceptsICInt(i: Int, ic: ICInt) {}
inline fun ordinaryInlineAcceptsICIntNullable(i: Int, ic: ICIntNullable) {}
inline fun ordinaryInlineAcceptsICString_Null(i: Int, ic: ICString?) {}
inline fun ordinaryInlineAcceptsICStringNullable_Null(i: Int, ic: ICStringNullable?) {}
inline fun ordinaryInlineAcceptsICAny_Null(i: Int, ic: ICAny?) {}
inline fun ordinaryInlineAcceptsICAnyNullable_Null(i: Int, ic: ICAnyNullable?) {}
inline fun ordinaryInlineAcceptsICInt_Null(i: Int, ic: ICInt?) {}
inline fun ordinaryInlineAcceptsICIntNullable_Null(i: Int, ic: ICIntNullable?) {}
suspend fun suspendNoninlineReturnsICString(): ICString = ICString("")
suspend fun suspendNoninlineReturnsICStringNullable(): ICStringNullable = ICStringNullable("")
suspend fun suspendNoninlineReturnsICAny(): ICAny = ICAny("")
suspend fun suspendNoninlineReturnsICAnyNullable(): ICAnyNullable = ICAnyNullable("")
suspend fun suspendNoninlineReturnsICInt(): ICInt = ICInt(0)
suspend fun suspendNoninlineReturnsICIntNullable(): ICIntNullable = ICIntNullable(0)
suspend fun suspendNoninlineReturnsICString_Null(): ICString? = null
suspend fun suspendNoninlineReturnsICStringNullable_Null(): ICStringNullable? = null
suspend fun suspendNoninlineReturnsICAny_Null(): ICAny? = null
suspend fun suspendNoninlineReturnsICAnyNullable_Null(): ICAnyNullable? = null
suspend fun suspendNoninlineReturnsICInt_Null(): ICInt? = null
suspend fun suspendNoninlineReturnsICIntNullable_Null(): ICIntNullable? = null
suspend fun suspendNoninlineAcceptsICString(i: Int, ic: ICString) {}
suspend fun suspendNoninlineAcceptsICStringNullable(i: Int, ic: ICStringNullable) {}
suspend fun suspendNoninlineAcceptsICAny(i: Int, ic: ICAny) {}
suspend fun suspendNoninlineAcceptsICAnyNullable(i: Int, ic: ICAnyNullable) {}
suspend fun suspendNoninlineAcceptsICInt(i: Int, ic: ICInt) {}
suspend fun suspendNoninlineAcceptsICIntNullable(i: Int, ic: ICIntNullable) {}
suspend fun suspendNoninlineAcceptsICString_Null(i: Int, ic: ICString?) {}
suspend fun suspendNoninlineAcceptsICStringNullable_Null(i: Int, ic: ICStringNullable?) {}
suspend fun suspendNoninlineAcceptsICAny_Null(i: Int, ic: ICAny?) {}
suspend fun suspendNoninlineAcceptsICAnyNullable_Null(i: Int, ic: ICAnyNullable?) {}
suspend fun suspendNoninlineAcceptsICInt_Null(i: Int, ic: ICInt?) {}
suspend fun suspendNoninlineAcceptsICIntNullable_Null(i: Int, ic: ICIntNullable?) {}
suspend inline fun suspendInlineReturnsICString(): ICString = ICString("")
suspend inline fun suspendInlineReturnsICStringNullable(): ICStringNullable = ICStringNullable("")
suspend inline fun suspendInlineReturnsICAny(): ICAny = ICAny("")
suspend inline fun suspendInlineReturnsICAnyNullable(): ICAnyNullable = ICAnyNullable("")
suspend inline fun suspendInlineReturnsICInt(): ICInt = ICInt(0)
suspend inline fun suspendInlineReturnsICIntNullable(): ICIntNullable = ICIntNullable(0)
suspend inline fun suspendInlineReturnsICString_Null(): ICString? = null
suspend inline fun suspendInlineReturnsICStringNullable_Null(): ICStringNullable? = null
suspend inline fun suspendInlineReturnsICAny_Null(): ICAny? = null
suspend inline fun suspendInlineReturnsICAnyNullable_Null(): ICAnyNullable? = null
suspend inline fun suspendInlineReturnsICInt_Null(): ICInt? = null
suspend inline fun suspendInlineReturnsICIntNullable_Null(): ICIntNullable? = null
suspend inline fun suspendInlineAcceptsICString(i: Int, ic: ICString) {}
suspend inline fun suspendInlineAcceptsICStringNullable(i: Int, ic: ICStringNullable) {}
suspend inline fun suspendInlineAcceptsICAny(i: Int, ic: ICAny) {}
suspend inline fun suspendInlineAcceptsICAnyNullable(i: Int, ic: ICAnyNullable) {}
suspend inline fun suspendInlineAcceptsICInt(i: Int, ic: ICInt) {}
suspend inline fun suspendInlineAcceptsICIntNullable(i: Int, ic: ICIntNullable) {}
suspend inline fun suspendInlineAcceptsICString_Null(i: Int, ic: ICString?) {}
suspend inline fun suspendInlineAcceptsICStringNullable_Null(i: Int, ic: ICStringNullable?) {}
suspend inline fun suspendInlineAcceptsICAny_Null(i: Int, ic: ICAny?) {}
suspend inline fun suspendInlineAcceptsICAnyNullable_Null(i: Int, ic: ICAnyNullable?) {}
suspend inline fun suspendInlineAcceptsICInt_Null(i: Int, ic: ICInt?) {}
suspend inline fun suspendInlineAcceptsICIntNullable_Null(i: Int, ic: ICIntNullable?) {}

class C {
    fun ordinaryNoninlineReturnsICString(): ICString = ICString("")
    fun ordinaryNoninlineReturnsICStringNullable(): ICStringNullable = ICStringNullable("")
    fun ordinaryNoninlineReturnsICAny(): ICAny = ICAny("")
    fun ordinaryNoninlineReturnsICAnyNullable(): ICAnyNullable = ICAnyNullable("")
    fun ordinaryNoninlineReturnsICInt(): ICInt = ICInt(0)
    fun ordinaryNoninlineReturnsICIntNullable(): ICIntNullable = ICIntNullable(0)
    fun ordinaryNoninlineReturnsICString_Null(): ICString? = null
    fun ordinaryNoninlineReturnsICStringNullable_Null(): ICStringNullable? = null
    fun ordinaryNoninlineReturnsICAny_Null(): ICAny? = null
    fun ordinaryNoninlineReturnsICAnyNullable_Null(): ICAnyNullable? = null
    fun ordinaryNoninlineReturnsICInt_Null(): ICInt? = null
    fun ordinaryNoninlineReturnsICIntNullable_Null(): ICIntNullable? = null
    fun ordinaryNoninlineAcceptsICString(i: Int, ic: ICString) {}
    fun ordinaryNoninlineAcceptsICStringNullable(i: Int, ic: ICStringNullable) {}
    fun ordinaryNoninlineAcceptsICAny(i: Int, ic: ICAny) {}
    fun ordinaryNoninlineAcceptsICAnyNullable(i: Int, ic: ICAnyNullable) {}
    fun ordinaryNoninlineAcceptsICInt(i: Int, ic: ICInt) {}
    fun ordinaryNoninlineAcceptsICIntNullable(i: Int, ic: ICIntNullable) {}
    fun ordinaryNoninlineAcceptsICString_Null(i: Int, ic: ICString?) {}
    fun ordinaryNoninlineAcceptsICStringNullable_Null(i: Int, ic: ICStringNullable?) {}
    fun ordinaryNoninlineAcceptsICAny_Null(i: Int, ic: ICAny?) {}
    fun ordinaryNoninlineAcceptsICAnyNullable_Null(i: Int, ic: ICAnyNullable?) {}
    fun ordinaryNoninlineAcceptsICInt_Null(i: Int, ic: ICInt?) {}
    fun ordinaryNoninlineAcceptsICIntNullable_Null(i: Int, ic: ICIntNullable?) {}
    inline fun ordinaryInlineReturnsICString(): ICString = ICString("")
    inline fun ordinaryInlineReturnsICStringNullable(): ICStringNullable = ICStringNullable("")
    inline fun ordinaryInlineReturnsICAny(): ICAny = ICAny("")
    inline fun ordinaryInlineReturnsICAnyNullable(): ICAnyNullable = ICAnyNullable("")
    inline fun ordinaryInlineReturnsICInt(): ICInt = ICInt(0)
    inline fun ordinaryInlineReturnsICIntNullable(): ICIntNullable = ICIntNullable(0)
    inline fun ordinaryInlineReturnsICString_Null(): ICString? = null
    inline fun ordinaryInlineReturnsICStringNullable_Null(): ICStringNullable? = null
    inline fun ordinaryInlineReturnsICAny_Null(): ICAny? = null
    inline fun ordinaryInlineReturnsICAnyNullable_Null(): ICAnyNullable? = null
    inline fun ordinaryInlineReturnsICInt_Null(): ICInt? = null
    inline fun ordinaryInlineReturnsICIntNullable_Null(): ICIntNullable? = null
    inline fun ordinaryInlineAcceptsICString(i: Int, ic: ICString) {}
    inline fun ordinaryInlineAcceptsICStringNullable(i: Int, ic: ICStringNullable) {}
    inline fun ordinaryInlineAcceptsICAny(i: Int, ic: ICAny) {}
    inline fun ordinaryInlineAcceptsICAnyNullable(i: Int, ic: ICAnyNullable) {}
    inline fun ordinaryInlineAcceptsICInt(i: Int, ic: ICInt) {}
    inline fun ordinaryInlineAcceptsICIntNullable(i: Int, ic: ICIntNullable) {}
    inline fun ordinaryInlineAcceptsICString_Null(i: Int, ic: ICString?) {}
    inline fun ordinaryInlineAcceptsICStringNullable_Null(i: Int, ic: ICStringNullable?) {}
    inline fun ordinaryInlineAcceptsICAny_Null(i: Int, ic: ICAny?) {}
    inline fun ordinaryInlineAcceptsICAnyNullable_Null(i: Int, ic: ICAnyNullable?) {}
    inline fun ordinaryInlineAcceptsICInt_Null(i: Int, ic: ICInt?) {}
    inline fun ordinaryInlineAcceptsICIntNullable_Null(i: Int, ic: ICIntNullable?) {}
    suspend fun suspendNoninlineReturnsICString(): ICString = ICString("")
    suspend fun suspendNoninlineReturnsICStringNullable(): ICStringNullable = ICStringNullable("")
    suspend fun suspendNoninlineReturnsICAny(): ICAny = ICAny("")
    suspend fun suspendNoninlineReturnsICAnyNullable(): ICAnyNullable = ICAnyNullable("")
    suspend fun suspendNoninlineReturnsICInt(): ICInt = ICInt(0)
    suspend fun suspendNoninlineReturnsICIntNullable(): ICIntNullable = ICIntNullable(0)
    suspend fun suspendNoninlineReturnsICString_Null(): ICString? = null
    suspend fun suspendNoninlineReturnsICStringNullable_Null(): ICStringNullable? = null
    suspend fun suspendNoninlineReturnsICAny_Null(): ICAny? = null
    suspend fun suspendNoninlineReturnsICAnyNullable_Null(): ICAnyNullable? = null
    suspend fun suspendNoninlineReturnsICInt_Null(): ICInt? = null
    suspend fun suspendNoninlineReturnsICIntNullable_Null(): ICIntNullable? = null
    suspend fun suspendNoninlineAcceptsICString(i: Int, ic: ICString) {}
    suspend fun suspendNoninlineAcceptsICStringNullable(i: Int, ic: ICStringNullable) {}
    suspend fun suspendNoninlineAcceptsICAny(i: Int, ic: ICAny) {}
    suspend fun suspendNoninlineAcceptsICAnyNullable(i: Int, ic: ICAnyNullable) {}
    suspend fun suspendNoninlineAcceptsICInt(i: Int, ic: ICInt) {}
    suspend fun suspendNoninlineAcceptsICIntNullable(i: Int, ic: ICIntNullable) {}
    suspend fun suspendNoninlineAcceptsICString_Null(i: Int, ic: ICString?) {}
    suspend fun suspendNoninlineAcceptsICStringNullable_Null(i: Int, ic: ICStringNullable?) {}
    suspend fun suspendNoninlineAcceptsICAny_Null(i: Int, ic: ICAny?) {}
    suspend fun suspendNoninlineAcceptsICAnyNullable_Null(i: Int, ic: ICAnyNullable?) {}
    suspend fun suspendNoninlineAcceptsICInt_Null(i: Int, ic: ICInt?) {}
    suspend fun suspendNoninlineAcceptsICIntNullable_Null(i: Int, ic: ICIntNullable?) {}
    suspend inline fun suspendInlineReturnsICString(): ICString = ICString("")
    suspend inline fun suspendInlineReturnsICStringNullable(): ICStringNullable = ICStringNullable("")
    suspend inline fun suspendInlineReturnsICAny(): ICAny = ICAny("")
    suspend inline fun suspendInlineReturnsICAnyNullable(): ICAnyNullable = ICAnyNullable("")
    suspend inline fun suspendInlineReturnsICInt(): ICInt = ICInt(0)
    suspend inline fun suspendInlineReturnsICIntNullable(): ICIntNullable = ICIntNullable(0)
    suspend inline fun suspendInlineReturnsICString_Null(): ICString? = null
    suspend inline fun suspendInlineReturnsICStringNullable_Null(): ICStringNullable? = null
    suspend inline fun suspendInlineReturnsICAny_Null(): ICAny? = null
    suspend inline fun suspendInlineReturnsICAnyNullable_Null(): ICAnyNullable? = null
    suspend inline fun suspendInlineReturnsICInt_Null(): ICInt? = null
    suspend inline fun suspendInlineReturnsICIntNullable_Null(): ICIntNullable? = null
    suspend inline fun suspendInlineAcceptsICString(i: Int, ic: ICString) {}
    suspend inline fun suspendInlineAcceptsICStringNullable(i: Int, ic: ICStringNullable) {}
    suspend inline fun suspendInlineAcceptsICAny(i: Int, ic: ICAny) {}
    suspend inline fun suspendInlineAcceptsICAnyNullable(i: Int, ic: ICAnyNullable) {}
    suspend inline fun suspendInlineAcceptsICInt(i: Int, ic: ICInt) {}
    suspend inline fun suspendInlineAcceptsICIntNullable(i: Int, ic: ICIntNullable) {}
    suspend inline fun suspendInlineAcceptsICString_Null(i: Int, ic: ICString?) {}
    suspend inline fun suspendInlineAcceptsICStringNullable_Null(i: Int, ic: ICStringNullable?) {}
    suspend inline fun suspendInlineAcceptsICAny_Null(i: Int, ic: ICAny?) {}
    suspend inline fun suspendInlineAcceptsICAnyNullable_Null(i: Int, ic: ICAnyNullable?) {}
    suspend inline fun suspendInlineAcceptsICInt_Null(i: Int, ic: ICInt?) {}
    suspend inline fun suspendInlineAcceptsICIntNullable_Null(i: Int, ic: ICIntNullable?) {}
}