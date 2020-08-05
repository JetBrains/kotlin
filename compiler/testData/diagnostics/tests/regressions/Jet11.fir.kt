// JET-11 Redeclaration & Forward reference for classes cause an exception
<!REDECLARATION!>open class NoC<!>
class NoC1 : NoC()
<!REDECLARATION!>open class NoC<!>
