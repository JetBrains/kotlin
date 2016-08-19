/**
 * Created by user on 8/18/16.
 */
class McState {


    private var connected = false


    fun isConnected(): Boolean {
        return connected
    }

    fun connect() {
        this.connected = true
    }


    companion object {
        val instance = McState()
    }

}