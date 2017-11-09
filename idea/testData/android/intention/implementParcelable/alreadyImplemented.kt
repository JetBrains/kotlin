// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.ImplementParcelableAction
// NOT_AVAILABLE
import android.os.Parcel
import android.os.Parcelable


class <caret>Simple(parcel: Parcel) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Simple> {
        override fun createFromParcel(parcel: Parcel): Simple {
            return Simple(parcel)
        }

        override fun newArray(size: Int): Array<Simple?> {
            return arrayOfNulls(size)
        }
    }
}