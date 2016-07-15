	.text
	.file	"src/kotlin_main.ll"
	.globl	test
	.align	16, 0x90
	.type	test,@function
test:                                   # @test
# BB#0:
	leal	1(%rdi), %eax
	movl	%edi, -4(%rsp)
	retq
.Lfunc_end0:
	.size	test, .Lfunc_end0-test

	.globl	kotlin_main
	.align	16, 0x90
	.type	kotlin_main,@function
kotlin_main:                            # @kotlin_main
# BB#0:
	pushq	%rbp
	movq	%rsp, %rbp
	subq	$16, %rsp
	movl	$0, -4(%rbp)
	xorl	%edi, %edi
	callq	test
	movl	%eax, -8(%rbp)
	jmp	.LBB1_1
	.align	16, 0x90
.LBB1_2:                                # %label.while.2
                                        #   in Loop: Header=BB1_1 Depth=1
	movl	-4(%rbp), %edi
	callq	test
	movq	%rsp, %rcx
	leaq	-16(%rcx), %rsp
	movl	%eax, -16(%rcx)
.LBB1_1:                                # %label.while.1
                                        # =>This Inner Loop Header: Depth=1
	movl	%eax, -4(%rbp)
	cmpl	$100, -4(%rbp)
	jne	.LBB1_2
# BB#3:                                 # %label.while.3
	xorl	%eax, %eax
	movq	%rbp, %rsp
	popq	%rbp
	retq
.Lfunc_end1:
	.size	kotlin_main, .Lfunc_end1-kotlin_main


	.section	".note.GNU-stack","",@progbits
