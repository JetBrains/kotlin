declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc(i32)
%class.MyAwesomeClass = type { i32 }
define void @MyAwesomeClass(%class.MyAwesomeClass*  %classvariable.this, i32  %i) 
{
%classvariable.this.addr = alloca %class.MyAwesomeClass, align 4
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%var1 = load i32* %i.addr, align 4
%var2 = getelementptr inbounds %class.MyAwesomeClass* %classvariable.this.addr, i32 0, i32 0
store i32 %var1, i32* %var2, align 4
%var3 = bitcast %class.MyAwesomeClass* %classvariable.this to i8*
%var4 = bitcast %class.MyAwesomeClass* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var3, i8* %var4, i64 4, i32 4, i1 false)
ret void 
}
define i32 @nullable_test(i32  %i) 
{
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%var6 = call i8* @malloc(i32 4)
%var5 = bitcast i8* %var6 to %class.MyAwesomeClass*
call void @MyAwesomeClass(%class.MyAwesomeClass* %var5, i32 1)
%var8 = call i8* @malloc(i32 4)
%var7 = bitcast i8* %var8 to %class.MyAwesomeClass*
call void @MyAwesomeClass(%class.MyAwesomeClass* %var7, i32 2)
%var9 = load %class.MyAwesomeClass* %var5, align 4
%var10 = load %class.MyAwesomeClass* %var7, align 4
%var11 = getelementptr inbounds %class.MyAwesomeClass* %var7, i32 0, i32 0
%var12 = load i32* %var11, align 4
ret i32 %var12
}
define void @main() 
{
%var13 = call i32 @nullable_test(i32 0)
%var14 = alloca i32, align 4
store i32 %var13, i32* %var14, align 4
ret void 
}

