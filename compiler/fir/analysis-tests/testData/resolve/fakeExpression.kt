// RUN_PIPELINE_TILL: FRONTEND
import java.util.<!UNRESOLVED_IMPORT!>function<!>.Supplier;

public abstract class SuspendContextImpl <!SYNTAX!>extends<!> <!SYNTAX!>Disposable<!> <!SYNTAX!><!>{
  @<!UNRESOLVED_REFERENCE!>MagicConstant<!>(flagsFromClass = <!UNRESOLVED_REFERENCE!>MyClass<!>.class<!SYNTAX!>)<!><!SYNTAX!><!>
  @Override
  <!UNRESOLVED_REFERENCE!>public<!> int <!UNRESOLVED_REFERENCE!>abc<!>() {
    <!RETURN_NOT_ALLOWED!>return<!> <!RETURN_TYPE_MISMATCH!>0<!>;
  }

  private <!UNRESOLVED_REFERENCE!>static<!> final <!NO_COMPANION_OBJECT!>Comparator<<!UNRESOLVED_REFERENCE!>A<!>><!> COMPARATOR_A <!SYNTAX!>=<!> foo(<!SYNTAX!><!>)<!SYNTAX!><!>;

  <!UNRESOLVED_REFERENCE!>private<!> static <!UNRESOLVED_REFERENCE!>final<!> Comparator<!SYNTAX!><<!>B<!SYNTAX!>><!> COMPARATOR_B <!SYNTAX!>=<!> bar(<!SYNTAX!><!>);
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, lambdaLiteral */
