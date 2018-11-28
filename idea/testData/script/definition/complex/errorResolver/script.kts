/*
TestDependenciesResolver returns empty dependencies on first request,
so ComparasionFailure should fail because 'test' is unresolved
Than TestDependenciesResolver returns classpath with classes from 'lib' folder
so no errors expected
*/
test.KObject.foo()
