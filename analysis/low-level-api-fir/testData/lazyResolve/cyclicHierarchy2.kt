interface Resolve<caret>Me : A, E {}
interface C : ResolveMe {}
interface D : ResolveMe {}
interface E : F {}
interface F : D, C {}
interface NonLoopedInterface : C