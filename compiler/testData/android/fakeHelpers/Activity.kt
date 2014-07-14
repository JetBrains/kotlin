package android.app
import android.view.View

trait Activity {
    fun findViewById(id: Int): View?
}