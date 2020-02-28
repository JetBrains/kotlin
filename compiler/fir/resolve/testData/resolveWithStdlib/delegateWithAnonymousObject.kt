// !DUMP_CFG

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class DelegateProvider<in Type>

fun <Type : Base, Base : DelegateProvider<Base>, Target : Any> Type.delegate(
    factory: () -> ReadWriteProperty<Type, Target>
): ReadWriteProperty<Type, Target> = null!!

class IssueListView : DelegateProvider<IssueListView>() {
    fun updateFrom(any: Any) {}
}

class IssuesListUserProfile : DelegateProvider<IssuesListUserProfile>() {
    var issueListView by delegate {
        object : ReadWriteProperty<IssuesListUserProfile, IssueListView> {
            override fun getValue(thisRef: IssuesListUserProfile, property: KProperty<*>): IssueListView {
                return IssueListView()
            }

            override fun setValue(thisRef: IssuesListUserProfile, property: KProperty<*>, value: IssueListView) {
                return IssueListView().updateFrom(value)
            }
        }
    }
}