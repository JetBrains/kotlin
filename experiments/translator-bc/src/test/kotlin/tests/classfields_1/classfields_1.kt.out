@exceptions.str.1 = private unnamed_addr constant  [59 x i8] c"Exception in thread main kotlin.KotlinNullPointerException\00", align 1
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc_heap(i32)
declare i32 @printf(i8*, ...)
declare void @abort()
%class.Nothing = type { }
%class.WithFields = type { i32, i32 }
define weak void @WithFields_Int(%class.WithFields*  %classvariable.this, i32  %WithFields.i) #0
{
%classvariable.this.addr = alloca %class.WithFields, align 8
%WithFields.i.addr = alloca i32, align 4
store i32 %WithFields.i, i32* %WithFields.i.addr, align 4
%var2 = load i32* %WithFields.i.addr, align 4
%var3 = getelementptr inbounds %class.WithFields* %classvariable.this.addr, i32 0, i32 0
store i32 %var2, i32* %var3, align 4
%var4 = bitcast %class.WithFields* %classvariable.this to i8*
%var5 = bitcast %class.WithFields* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var4, i8* %var5, i64 8, i32 4, i1 false)
%var6 = getelementptr inbounds %class.WithFields* %classvariable.this, i32 0, i32 1
%var7 = getelementptr inbounds %class.WithFields* %classvariable.this, i32 0, i32 0
%var8 = load i32* %var6, align 4
%var9 = load i32* %var7, align 4
store i32 %var9, i32* %var6, align 4
ret void 
unreachable
}
define weak i32 @test_field_assignment_Int(i32  %test_field_assignment.i) #0
{
%test_field_assignment.i.addr = alloca i32, align 4
store i32 %test_field_assignment.i, i32* %test_field_assignment.i.addr, align 4
%var10 = load i32* %test_field_assignment.i.addr, align 4
%var12 = call i8* @malloc_heap(i32 8)
%var11 = bitcast i8* %var12 to %class.WithFields*
%var13 = alloca %class.WithFields*, align 8
store %class.WithFields* %var11, %class.WithFields** %var13, align 8
call void @WithFields_Int(%class.WithFields* %var11, i32 %var10)
%managed.unique.0.test_field_assignment.k = alloca %class.WithFields*, align 8
%var14 = load %class.WithFields** %managed.unique.0.test_field_assignment.k, align 8
%var15 = load %class.WithFields** %var13, align 8
store %class.WithFields* %var15, %class.WithFields** %managed.unique.0.test_field_assignment.k, align 8
%var16 = load %class.WithFields** %managed.unique.0.test_field_assignment.k, align 8
%var17 = getelementptr inbounds %class.WithFields* %var16, i32 0, i32 1
%var18 = load i32* %var17, align 4
ret i32 %var18
unreachable
}
define weak i32 @test_simple_field() #0
{
%var20 = call i8* @malloc_heap(i32 8)
%var19 = bitcast i8* %var20 to %class.WithFields*
%var21 = alloca %class.WithFields*, align 8
store %class.WithFields* %var19, %class.WithFields** %var21, align 8
call void @WithFields_Int(%class.WithFields* %var19, i32 1)
%managed.unique.1.test_simple_field.i = alloca %class.WithFields*, align 8
%var22 = load %class.WithFields** %managed.unique.1.test_simple_field.i, align 8
%var23 = load %class.WithFields** %var21, align 8
store %class.WithFields* %var23, %class.WithFields** %managed.unique.1.test_simple_field.i, align 8
%var24 = load %class.WithFields** %managed.unique.1.test_simple_field.i, align 8
%var25 = getelementptr inbounds %class.WithFields* %var24, i32 0, i32 1
%var26 = load i32* %var25, align 4
ret i32 %var26
unreachable
}

