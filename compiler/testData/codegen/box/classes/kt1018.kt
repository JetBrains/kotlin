public class StockMarketTableModel() {

    public fun getColumnCount() : Int {
        return COLUMN_TITLES?.size!!
    }

    companion object {
        private val COLUMN_TITLES : Array<Int?> = arrayOfNulls<Int>(10)
    }
}

fun box() : String = if(StockMarketTableModel().getColumnCount()==10) "OK" else "fail"
