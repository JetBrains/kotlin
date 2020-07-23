// !WITH_NEW_INFERENCE
import java.util.ArrayList

<!UNRESOLVED_REFERENCE!>@ArrayList<Int>(1, 1)<!> fun b() {}
<!UNRESOLVED_REFERENCE!>@<!UNRESOLVED_REFERENCE!>Xoo<!>(<!UNRESOLVED_REFERENCE!>x<!>)<!> fun c() {}
<!INAPPLICABLE_CANDIDATE!>@java.lang.Deprecated(<!UNRESOLVED_REFERENCE!>x<!>)<!> fun a() {}