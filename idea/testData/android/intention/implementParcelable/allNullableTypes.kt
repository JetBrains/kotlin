// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.ImplementParcelableAction

import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseBooleanArray
import android.util.SparseIntArray

open class BaseParcelable(parcel: Parcel) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BaseParcelable> {
        override fun createFromParcel(parcel: Parcel): BaseParcelable {
            return BaseParcelable(parcel)
        }

        override fun newArray(size: Int): Array<BaseParcelable?> {
            return arrayOfNulls(size)
        }
    }
}

class <caret>MyData {

    val delegated by lazy { 0 }
    val readonlyField = true

    var listOfStrings: List<String?>? = null
    var listOfIBinder: List<IBinder?>? = null

    var arrayfield1: IntArray? = null
    var arrayfield2: Array<String>? = null
    var arrayfield3: LongArray? = null
    var arrayfield4: ByteArray? = null
    var arrayfield5: CharArray? = null
    var arrayfield6: ShortArray? = null // No method for Short
    var arrayfield7: FloatArray? = null
    var arrayfield8: DoubleArray? = null
    var arrayfield9: BooleanArray? = null
    var arrayfield10: Array<CharSequence>? = null // No method for CharSequence
    var arrayfield11: Array<IBinder>? = null

    var field1: Int? = 0
    var field2: String? = ""
    var field3: Long? = 0
    var field4: Byte? = 0
    var field5: Char? = '\u0000'
    var field6: Short? = 0
    var field7: Float? = 0.0f
    var field8: Double? = 0.0
    var field9: Boolean? = false
    var field10: CharSequence? = ""
    var field11: SparseBooleanArray? = null
    var field12: SparseIntArray? = null  // read/write methods will be available starting from android O
    var field13: Bundle? = null
    var field14: IBinder? = null

    var goodArray: Array<BaseParcelable?>? = emptyArray()
    var badArray: Array<Parcelable?>? = emptyArray()

    var goodList: List<BaseParcelable?>? = emptyList()
    var badList: List<Parcelable?>? = emptyList()

    var parcelableProperty: BaseParcelable?
    val uninitializedVal: Int?

    val fieldWIthGetter: Int?
    get() {
        return 0
    }

    var fieldWithCustomGetterAndSetter: Int? = 0
    get() {
        return field
    }
    set(value: Int?) {
        field = value
    }
}