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

!llvm.dbg.cu = !{!0}
!llvm.module.flags = !{!1, !2}
!llvm.ident = !{!3}

!0 = distinct !{!"0x11\000x801\00cotkat 1.0 3015852d7a7854269c4d3e906a7b97bf0d365a94\000\00\000\00\001", !4, !6, !6}
!1 = !{i32 2, !"Dwarf Version", i32 2}
!2 = !{i32 2, !"Debug Info Version", i32 2}
!3 =!{!"LLVM (http://llvm.org/): LLVM version 3.6.2 Optimized build. Built Jul 28 2016 (03:09:45). Default target: x86_64-apple-darwin15.6.0 Host CPU: core-avx2"}
!4 = !{!"classfields_1.kt", !"../translator/"}
!5 = !{!"0x2e\00\00test_simple_field_Int\00test_simple_field_Int\00\001\000\001\000\000\000\001", !4, !6, null, i32 (i32)* @test_field_assignment_Int, null, null, null, !7 } ; [DW_TAG_subprogram]
!6 = !{!"0x29", !4} ; what is this ?
!7 = !{} ; Huh?!
!8 = !{!"0x15\00\000\000\000\000\000\000", null, null, null, !9, null, null, null} ; [ DW_TAG_subroutine_type ] [line 0, size 0, align 0, offset 0] [from ]
!9 = !{!10}
!10 = !{!"0x24\00int\000\0032\0032\000\000\005", null, null} ; [ DW_TAG_base_type ] [int] [line 0, size 32, align 32, offset 0, enc DW_ATE_signed]
