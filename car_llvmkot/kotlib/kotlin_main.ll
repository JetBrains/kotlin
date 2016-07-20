declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc(i32)
%class.Outer = type {  }
define void @Outer(%class.Outer*  %classvariable.this) 
{
%classvariable.this.addr = alloca %class.Outer, align 4
%var1 = bitcast %class.Outer* %classvariable.this to i8*
%var2 = bitcast %class.Outer* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var1, i8* %var2, i64 0, i32 4, i1 false)
ret void 
}
define void @Outer.main(%class.Outer*  %instance, %class.Outer*  %classvariable.this) 
{
%var4 = call i8* @malloc(i32 0)
%var3 = bitcast i8* %var4 to %class.Outer*
call void @Outer(%class.Outer* %var3)
%var5 = bitcast %class.Outer* %var3 to i8*
%var6 = bitcast %class.Outer* %instance to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var6, i8* %var5, i64 0, i32 4, i1 false)
ret void 
}
%class.Outer_Nested = type { i32 }
define void @Outer_Nested(%class_Outer.Nested*  %classvariable.this) 
{
%classvariable.this.addr = alloca %class_Outer.Nested, align 4
%var7 = bitcast %class_Outer.Nested* %classvariable.this to i8*
%var8 = bitcast %class_Outer.Nested* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var7, i8* %var8, i64 4, i32 4, i1 false)
ret void 
}
define void @Outer.Nested.main(%class_Outer.Nested*  %classvariable.this) 
{
ret void 
}
define void @test() 
{
ret void 
}

