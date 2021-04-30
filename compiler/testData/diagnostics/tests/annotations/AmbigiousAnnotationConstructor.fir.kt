// !WITH_NEW_INFERENCE
import java.util.ArrayList

@<!NOT_AN_ANNOTATION_CLASS!>ArrayList<!><Int>(1, 1) fun b() {}
@<!UNRESOLVED_REFERENCE!>Xoo<!>(<!UNRESOLVED_REFERENCE!>x<!>) fun c() {}
@java.lang.Deprecated(<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>x<!>) fun a() {}
