public class StockMarketTableModel() {

    public fun getColumnCount() : Int {
        return COLUMN_TITLES?.size()!!
    }

    default object {
        private val COLUMN_TITLES : Array<Int?> = arrayOfNulls<Int>(10)
    }
}

fun box() : String = if(StockMarketTableModel().getColumnCount()==10) "OK" else "fail"
