/**
  ******************************************************************************
  * @file    stm32f4xx_cryp.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the  Cryptographic processor (CRYP) peripheral:           
  *           - Initialization and Configuration functions
  *           - Data treatment functions 
  *           - Context swapping functions     
  *           - DMA interface function       
  *           - Interrupts and flags management       
  *
  *  @verbatim
  *                               
  *          ===================================================================      
  *                                 How to use this driver
  *          =================================================================== 
  *          1. Enable the CRYP controller clock using 
  *              RCC_AHB2PeriphClockCmd(RCC_AHB2Periph_CRYP, ENABLE); function.
  *
  *          2. Initialise the CRYP using CRYP_Init(), CRYP_KeyInit() and if 
  *             needed CRYP_IVInit(). 
  *
  *          3. Flush the IN and OUT FIFOs by using CRYP_FIFOFlush() function.
  *
  *          4. Enable the CRYP controller using the CRYP_Cmd() function. 
  *
  *          5. If using DMA for Data input and output transfer, 
  *             Activate the needed DMA Requests using CRYP_DMACmd() function 
  
  *          6. If DMA is not used for data transfer, use CRYP_DataIn() and 
  *             CRYP_DataOut() functions to enter data to IN FIFO and get result
  *             from OUT FIFO.
  *
  *          7. To control CRYP events you can use one of the following 
  *              two methods:
  *               - Check on CRYP flags using the CRYP_GetFlagStatus() function.  
  *               - Use CRYP interrupts through the function CRYP_ITConfig() at 
  *                 initialization phase and CRYP_GetITStatus() function into 
  *                 interrupt routines in processing phase.
  *       
  *          8. Save and restore Cryptographic processor context using  
  *             CRYP_SaveContext() and CRYP_RestoreContext() functions.     
  *
  *
  *          ===================================================================  
  *                Procedure to perform an encryption or a decryption
  *          ===================================================================  
  *
  *      Initialization
  *      ===============  
  *     1. Initialize the peripheral using CRYP_Init(), CRYP_KeyInit() and 
  *        CRYP_IVInit functions:
  *        - Configure the key size (128-, 192- or 256-bit, in the AES only) 
  *        - Enter the symmetric key 
  *        - Configure the data type
  *        - In case of decryption in AES-ECB or AES-CBC, you must prepare 
  *          the key: configure the key preparation mode. Then Enable the CRYP 
  *          peripheral using CRYP_Cmd() function: the BUSY flag is set. 
  *          Wait until BUSY flag is reset : the key is prepared for decryption
  *       - Configure the algorithm and chaining (the DES/TDES in ECB/CBC, the 
  *          AES in ECB/CBC/CTR) 
  *       - Configure the direction (encryption/decryption).
  *       - Write the initialization vectors (in CBC or CTR modes only)
  *
  *    2. Flush the IN and OUT FIFOs using the CRYP_FIFOFlush() function
  *
  *
  *    Basic Processing mode (polling mode) 
  *    ====================================  
  *    1. Enable the cryptographic processor using CRYP_Cmd() function.
  *
  *    2. Write the first blocks in the input FIFO (2 to 8 words) using 
  *       CRYP_DataIn() function.
  *
  *    3. Repeat the following sequence until the complete message has been 
  *       processed:
  *
  *       a) Wait for flag CRYP_FLAG_OFNE occurs (using CRYP_GetFlagStatus() 
  *          function), then read the OUT-FIFO using CRYP_DataOut() function
  *          (1 block or until the FIFO is empty)
  *
  *       b) Wait for flag CRYP_FLAG_IFNF occurs, (using CRYP_GetFlagStatus() 
  *          function then write the IN FIFO using CRYP_DataIn() function 
  *          (1 block or until the FIFO is full)
  *
  *    4. At the end of the processing, CRYP_FLAG_BUSY flag will be reset and 
  *        both FIFOs are empty (CRYP_FLAG_IFEM is set and CRYP_FLAG_OFNE is 
  *        reset). You can disable the peripheral using CRYP_Cmd() function.
  *
  *    Interrupts Processing mode 
  *    ===========================
  *    In this mode, Processing is done when the data are transferred by the 
  *    CPU during interrupts.
  *
  *    1. Enable the interrupts CRYP_IT_INI and CRYP_IT_OUTI using 
  *       CRYP_ITConfig() function.
  *
  *    2. Enable the cryptographic processor using CRYP_Cmd() function.
  *
  *    3. In the CRYP_IT_INI interrupt handler : load the input message into the 
  *       IN FIFO using CRYP_DataIn() function . You can load 2 or 4 words at a 
  *       time, or load data until the IN FIFO is full. When the last word of
  *       the message has been entered into the IN FIFO, disable the CRYP_IT_INI 
  *       interrupt (using CRYP_ITConfig() function).
  *
  *    4. In the CRYP_IT_OUTI interrupt handler : read the output message from 
  *       the OUT FIFO using CRYP_DataOut() function. You can read 1 block (2 or 
  *       4 words) at a time or read data until the FIFO is empty.
  *       When the last word has been read, INIM=0, BUSY=0 and both FIFOs are 
  *       empty (CRYP_FLAG_IFEM is set and CRYP_FLAG_OFNE is reset). 
  *       You can disable the CRYP_IT_OUTI interrupt (using CRYP_ITConfig() 
  *       function) and you can disable the peripheral using CRYP_Cmd() function.
  *
  *    DMA Processing mode 
  *    ====================
  *    In this mode, Processing is done when the DMA is used to transfer the 
  *    data from/to the memory.
  *
  *    1. Configure the DMA controller to transfer the input data from the 
  *       memory using DMA_Init() function. 
  *       The transfer length is the length of the message. 
  *       As message padding is not managed by the peripheral, the message 
  *       length must be an entire number of blocks. The data are transferred 
  *       in burst mode. The burst length is 4 words in the AES and 2 or 4 
  *       words in the DES/TDES. The DMA should be configured to set an 
  *       interrupt on transfer completion of the output data to indicate that 
  *       the processing is finished. 
  *       Refer to DMA peripheral driver for more details.  
  *
  *    2. Enable the cryptographic processor using CRYP_Cmd() function. 
  *       Enable the DMA requests CRYP_DMAReq_DataIN and CRYP_DMAReq_DataOUT 
  *       using CRYP_DMACmd() function.
  *
  *    3. All the transfers and processing are managed by the DMA and the 
  *       cryptographic processor. The DMA transfer complete interrupt indicates 
  *       that the processing is complete. Both FIFOs are normally empty and 
  *       CRYP_FLAG_BUSY flag is reset.
  *
  *  @endverbatim
  *
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************  
  */

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx_conf.h"
#include "stm32f4xx_cryp.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup CRYP 
  * @brief CRYP driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/
#define FLAG_MASK     ((uint8_t)0x20)
#define MAX_TIMEOUT   ((uint16_t)0xFFFF)

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup CRYP_Private_Functions
  * @{
  */ 

/** @defgroup CRYP_Group1 Initialization and Configuration functions
 *  @brief    Initialization and Configuration functions 
 *
@verbatim    
 ===============================================================================
                      Initialization and Configuration functions
 ===============================================================================  
  This section provides functions allowing to 
   - Initialize the cryptographic Processor using CRYP_Init() function 
      -  Encrypt or Decrypt 
      -  mode : TDES-ECB, TDES-CBC, 
                DES-ECB, DES-CBC, 
                AES-ECB, AES-CBC, AES-CTR, AES-Key 
      - DataType :  32-bit data, 16-bit data, bit data or bit-string
      - Key Size (only in AES modes)
   - Configure the Encrypt or Decrypt Key using CRYP_KeyInit() function 
   - Configure the Initialization Vectors(IV) for CBC and CTR modes using 
     CRYP_IVInit() function.  
   - Flushes the IN and OUT FIFOs : using CRYP_FIFOFlush() function.                         
   - Enable or disable the CRYP Processor using CRYP_Cmd() function 
    
   
@endverbatim
  * @{
  */
/**
  * @brief  Deinitializes the CRYP peripheral registers to their default reset values
  * @param  None
  * @retval None
  */
void CRYP_DeInit(void)
{
  /* Enable CRYP reset state */
  RCC_AHB2PeriphResetCmd(RCC_AHB2Periph_CRYP, ENABLE);

  /* Release CRYP from reset state */
  RCC_AHB2PeriphResetCmd(RCC_AHB2Periph_CRYP, DISABLE);
}

/**
  * @brief  Initializes the CRYP peripheral according to the specified parameters
  *         in the CRYP_InitStruct.
  * @param  CRYP_InitStruct: pointer to a CRYP_InitTypeDef structure that contains
  *         the configuration information for the CRYP peripheral.
  * @retval None
  */
void CRYP_Init(CRYP_InitTypeDef* CRYP_InitStruct)
{
  /* Check the parameters */
  assert_param(IS_CRYP_ALGOMODE(CRYP_InitStruct->CRYP_AlgoMode));
  assert_param(IS_CRYP_DATATYPE(CRYP_InitStruct->CRYP_DataType));
  assert_param(IS_CRYP_ALGODIR(CRYP_InitStruct->CRYP_AlgoDir));

  /* Select Algorithm mode*/  
  CRYP->CR &= ~CRYP_CR_ALGOMODE;
  CRYP->CR |= CRYP_InitStruct->CRYP_AlgoMode;

  /* Select dataType */ 
  CRYP->CR &= ~CRYP_CR_DATATYPE;
  CRYP->CR |= CRYP_InitStruct->CRYP_DataType;

  /* select Key size (used only with AES algorithm) */
  if ((CRYP_InitStruct->CRYP_AlgoMode == CRYP_AlgoMode_AES_ECB) ||
      (CRYP_InitStruct->CRYP_AlgoMode == CRYP_AlgoMode_AES_CBC) ||
      (CRYP_InitStruct->CRYP_AlgoMode == CRYP_AlgoMode_AES_CTR) ||
      (CRYP_InitStruct->CRYP_AlgoMode == CRYP_AlgoMode_AES_Key))
  {
    assert_param(IS_CRYP_KEYSIZE(CRYP_InitStruct->CRYP_KeySize));
    CRYP->CR &= ~CRYP_CR_KEYSIZE;
    CRYP->CR |= CRYP_InitStruct->CRYP_KeySize; /* Key size and value must be 
                                                  configured once the key has 
                                                  been prepared */
  }

  /* Select data Direction */ 
  CRYP->CR &= ~CRYP_CR_ALGODIR;
  CRYP->CR |= CRYP_InitStruct->CRYP_AlgoDir;
}

/**
  * @brief  Fills each CRYP_InitStruct member with its default value.
  * @param  CRYP_InitStruct: pointer to a CRYP_InitTypeDef structure which will
  *         be initialized.
  * @retval None
  */
void CRYP_StructInit(CRYP_InitTypeDef* CRYP_InitStruct)
{
  /* Initialize the CRYP_AlgoDir member */
  CRYP_InitStruct->CRYP_AlgoDir = CRYP_AlgoDir_Encrypt;

  /* initialize the CRYP_AlgoMode member */
  CRYP_InitStruct->CRYP_AlgoMode = CRYP_AlgoMode_TDES_ECB;

  /* initialize the CRYP_DataType member */
  CRYP_InitStruct->CRYP_DataType = CRYP_DataType_32b;
  
  /* Initialize the CRYP_KeySize member */
  CRYP_InitStruct->CRYP_KeySize = CRYP_KeySize_128b;
}

/**
  * @brief  Initializes the CRYP Keys according to the specified parameters in
  *         the CRYP_KeyInitStruct.
  * @param  CRYP_KeyInitStruct: pointer to a CRYP_KeyInitTypeDef structure that
  *         contains the configuration information for the CRYP Keys.
  * @retval None
  */
void CRYP_KeyInit(CRYP_KeyInitTypeDef* CRYP_KeyInitStruct)
{
  /* Key Initialisation */
  CRYP->K0LR = CRYP_KeyInitStruct->CRYP_Key0Left;
  CRYP->K0RR = CRYP_KeyInitStruct->CRYP_Key0Right;
  CRYP->K1LR = CRYP_KeyInitStruct->CRYP_Key1Left;
  CRYP->K1RR = CRYP_KeyInitStruct->CRYP_Key1Right;
  CRYP->K2LR = CRYP_KeyInitStruct->CRYP_Key2Left;
  CRYP->K2RR = CRYP_KeyInitStruct->CRYP_Key2Right;
  CRYP->K3LR = CRYP_KeyInitStruct->CRYP_Key3Left;
  CRYP->K3RR = CRYP_KeyInitStruct->CRYP_Key3Right;
}

/**
  * @brief  Fills each CRYP_KeyInitStruct member with its default value.
  * @param  CRYP_KeyInitStruct: pointer to a CRYP_KeyInitTypeDef structure 
  *         which will be initialized.
  * @retval None
  */
void CRYP_KeyStructInit(CRYP_KeyInitTypeDef* CRYP_KeyInitStruct)
{
  CRYP_KeyInitStruct->CRYP_Key0Left  = 0;
  CRYP_KeyInitStruct->CRYP_Key0Right = 0;
  CRYP_KeyInitStruct->CRYP_Key1Left  = 0;
  CRYP_KeyInitStruct->CRYP_Key1Right = 0;
  CRYP_KeyInitStruct->CRYP_Key2Left  = 0;
  CRYP_KeyInitStruct->CRYP_Key2Right = 0;
  CRYP_KeyInitStruct->CRYP_Key3Left  = 0;
  CRYP_KeyInitStruct->CRYP_Key3Right = 0;
}
/**
  * @brief  Initializes the CRYP Initialization Vectors(IV) according to the
  *         specified parameters in the CRYP_IVInitStruct.
  * @param  CRYP_IVInitStruct: pointer to a CRYP_IVInitTypeDef structure that contains
  *         the configuration information for the CRYP Initialization Vectors(IV).
  * @retval None
  */
void CRYP_IVInit(CRYP_IVInitTypeDef* CRYP_IVInitStruct)
{
  CRYP->IV0LR = CRYP_IVInitStruct->CRYP_IV0Left;
  CRYP->IV0RR = CRYP_IVInitStruct->CRYP_IV0Right;
  CRYP->IV1LR = CRYP_IVInitStruct->CRYP_IV1Left;
  CRYP->IV1RR = CRYP_IVInitStruct->CRYP_IV1Right;
}

/**
  * @brief  Fills each CRYP_IVInitStruct member with its default value.
  * @param  CRYP_IVInitStruct: pointer to a CRYP_IVInitTypeDef Initialization 
  *         Vectors(IV) structure which will be initialized.
  * @retval None
  */
void CRYP_IVStructInit(CRYP_IVInitTypeDef* CRYP_IVInitStruct)
{
  CRYP_IVInitStruct->CRYP_IV0Left  = 0;
  CRYP_IVInitStruct->CRYP_IV0Right = 0;
  CRYP_IVInitStruct->CRYP_IV1Left  = 0;
  CRYP_IVInitStruct->CRYP_IV1Right = 0;
}

/**
  * @brief  Flushes the IN and OUT FIFOs (that is read and write pointers of the 
  *         FIFOs are reset)
  * @note   The FIFOs must be flushed only when BUSY flag is reset.  
  * @param  None
  * @retval None
  */
void CRYP_FIFOFlush(void)
{
  /* Reset the read and write pointers of the FIFOs */
  CRYP->CR |= CRYP_CR_FFLUSH;
}

/**
  * @brief  Enables or disables the CRYP peripheral.
  * @param  NewState: new state of the CRYP peripheral.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void CRYP_Cmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Enable the Cryptographic processor */
    CRYP->CR |= CRYP_CR_CRYPEN;
  }
  else
  {
    /* Disable the Cryptographic processor */
    CRYP->CR &= ~CRYP_CR_CRYPEN;
  }
}
/**
  * @}
  */
  
/** @defgroup CRYP_Group2 CRYP Data processing functions
 *  @brief    CRYP Data processing functions
 *
@verbatim    
 ===============================================================================
                      CRYP Data processing functions
 ===============================================================================  
  This section provides functions allowing the encryption and decryption 
  operations: 
  - Enter data to be treated in the IN FIFO : using CRYP_DataIn() function.
  - Get the data result from the OUT FIFO : using CRYP_DataOut() function.

@endverbatim
  * @{
  */

/**
  * @brief  Writes data in the Data Input register (DIN).
  * @note   After the DIN register has been read once or several times, 
  *         the FIFO must be flushed (using CRYP_FIFOFlush() function).  
  * @param  Data: data to write in Data Input register
  * @retval None
  */
void CRYP_DataIn(uint32_t Data)
{
  CRYP->DR = Data;
}

/**
  * @brief  Returns the last data entered into the output FIFO.
  * @param  None
  * @retval Last data entered into the output FIFO.
  */
uint32_t CRYP_DataOut(void)
{
  return CRYP->DOUT;
}
/**
  * @}
  */
  
/** @defgroup CRYP_Group3 Context swapping functions
 *  @brief   Context swapping functions
 *
@verbatim   
 ===============================================================================
                             Context swapping functions
 ===============================================================================  

  This section provides functions allowing to save and store CRYP Context

  It is possible to interrupt an encryption/ decryption/ key generation process 
  to perform another processing with a higher priority, and to complete the 
  interrupted process later on, when the higher-priority task is complete. To do 
  so, the context of the interrupted task must be saved from the CRYP registers 
  to memory, and then be restored from memory to the CRYP registers.
   
  1. To save the current context, use CRYP_SaveContext() function
  2. To restore the saved context, use CRYP_RestoreContext() function 


@endverbatim
  * @{
  */
  
/**
  * @brief  Saves the CRYP peripheral Context. 
  * @note   This function stops DMA transfer before to save the context. After 
  *         restoring the context, you have to enable the DMA again (if the DMA
  *         was previously used).
  * @param  CRYP_ContextSave: pointer to a CRYP_Context structure that contains
  *         the repository for current context.
  * @param  CRYP_KeyInitStruct: pointer to a CRYP_KeyInitTypeDef structure that 
  *         contains the configuration information for the CRYP Keys.  
  * @retval None
  */
ErrorStatus CRYP_SaveContext(CRYP_Context* CRYP_ContextSave,
                             CRYP_KeyInitTypeDef* CRYP_KeyInitStruct)
{
  __IO uint32_t timeout = 0;
  uint32_t ckeckmask = 0, bitstatus;    
  ErrorStatus status = ERROR;

  /* Stop DMA transfers on the IN FIFO by clearing the DIEN bit in the CRYP_DMACR */
  CRYP->DMACR &= ~(uint32_t)CRYP_DMACR_DIEN;
    
  /* Wait until both the IN and OUT FIFOs are empty  
    (IFEM=1 and OFNE=0 in the CRYP_SR register) and the 
     BUSY bit is cleared. */

  if ((CRYP->CR & (uint32_t)(CRYP_CR_ALGOMODE_TDES_ECB | CRYP_CR_ALGOMODE_TDES_CBC)) != (uint32_t)0 )/* TDES */
  { 
    ckeckmask =  CRYP_SR_IFEM | CRYP_SR_BUSY ;
  }
  else /* AES or DES */
  {
    ckeckmask =  CRYP_SR_IFEM | CRYP_SR_BUSY | CRYP_SR_OFNE;
  }           
   
  do 
  {
    bitstatus = CRYP->SR & ckeckmask;
    timeout++;
  }
  while ((timeout != MAX_TIMEOUT) && (bitstatus != CRYP_SR_IFEM));
     
  if ((CRYP->SR & ckeckmask) != CRYP_SR_IFEM)
  {
    status = ERROR;
  }
  else
  {      
    /* Stop DMA transfers on the OUT FIFO by 
       - writing the DOEN bit to 0 in the CRYP_DMACR register 
       - and clear the CRYPEN bit. */

    CRYP->DMACR &= ~(uint32_t)CRYP_DMACR_DOEN;
    CRYP->CR &= ~(uint32_t)CRYP_CR_CRYPEN;

    /* Save the current configuration (bits [9:2] in the CRYP_CR register) */
    CRYP_ContextSave->CR_bits9to2  = CRYP->CR & (CRYP_CR_KEYSIZE  | 
                                                 CRYP_CR_DATATYPE | 
                                                 CRYP_CR_ALGOMODE |
                                                 CRYP_CR_ALGODIR); 

    /* and, if not in ECB mode, the initialization vectors. */
    CRYP_ContextSave->CRYP_IV0LR = CRYP->IV0LR;
    CRYP_ContextSave->CRYP_IV0RR = CRYP->IV0RR;
    CRYP_ContextSave->CRYP_IV1LR = CRYP->IV1LR;
    CRYP_ContextSave->CRYP_IV1RR = CRYP->IV1RR;

    /* save The key value */
    CRYP_ContextSave->CRYP_K0LR = CRYP_KeyInitStruct->CRYP_Key0Left; 
    CRYP_ContextSave->CRYP_K0RR = CRYP_KeyInitStruct->CRYP_Key0Right; 
    CRYP_ContextSave->CRYP_K1LR = CRYP_KeyInitStruct->CRYP_Key1Left; 
    CRYP_ContextSave->CRYP_K1RR = CRYP_KeyInitStruct->CRYP_Key1Right; 
    CRYP_ContextSave->CRYP_K2LR = CRYP_KeyInitStruct->CRYP_Key2Left; 
    CRYP_ContextSave->CRYP_K2RR = CRYP_KeyInitStruct->CRYP_Key2Right; 
    CRYP_ContextSave->CRYP_K3LR = CRYP_KeyInitStruct->CRYP_Key3Left; 
    CRYP_ContextSave->CRYP_K3RR = CRYP_KeyInitStruct->CRYP_Key3Right; 

   /* When needed, save the DMA status (pointers for IN and OUT messages, 
      number of remaining bytes, etc.) */
     
    status = SUCCESS;
  }

   return status;
}

/**
  * @brief  Restores the CRYP peripheral Context.
  * @note   Since teh DMA transfer is stopped in CRYP_SaveContext() function,
  *         after restoring the context, you have to enable the DMA again (if the
  *         DMA was previously used).  
  * @param  CRYP_ContextRestore: pointer to a CRYP_Context structure that contains
  *         the repository for saved context.
  * @note   The data that were saved during context saving must be rewrited into
  *         the IN FIFO.
  * @retval None
  */
void CRYP_RestoreContext(CRYP_Context* CRYP_ContextRestore)  
{

  /* Configure the processor with the saved configuration */
  CRYP->CR = CRYP_ContextRestore->CR_bits9to2;

  /* restore The key value */
  CRYP->K0LR = CRYP_ContextRestore->CRYP_K0LR; 
  CRYP->K0RR = CRYP_ContextRestore->CRYP_K0RR;
  CRYP->K1LR = CRYP_ContextRestore->CRYP_K1LR;
  CRYP->K1RR = CRYP_ContextRestore->CRYP_K1RR;
  CRYP->K2LR = CRYP_ContextRestore->CRYP_K2LR;
  CRYP->K2RR = CRYP_ContextRestore->CRYP_K2RR;
  CRYP->K3LR = CRYP_ContextRestore->CRYP_K3LR;
  CRYP->K3RR = CRYP_ContextRestore->CRYP_K3RR;

  /* and the initialization vectors. */
  CRYP->IV0LR = CRYP_ContextRestore->CRYP_IV0LR;
  CRYP->IV0RR = CRYP_ContextRestore->CRYP_IV0RR;
  CRYP->IV1LR = CRYP_ContextRestore->CRYP_IV1LR;
  CRYP->IV1RR = CRYP_ContextRestore->CRYP_IV1RR;

  /* Enable the cryptographic processor */
  CRYP->CR |= CRYP_CR_CRYPEN;
}
/**
  * @}
  */

/** @defgroup CRYP_Group4 CRYP's DMA interface Configuration function
 *  @brief   CRYP's DMA interface Configuration function 
 *
@verbatim   
 ===============================================================================
                   CRYP's DMA interface Configuration function
 ===============================================================================  

  This section provides functions allowing to configure the DMA interface for 
  CRYP data input and output transfer.
   
  When the DMA mode is enabled (using the CRYP_DMACmd() function), data can be 
  transferred:
  - From memory to the CRYP IN FIFO using the DMA peripheral by enabling 
    the CRYP_DMAReq_DataIN request.
  - From the CRYP OUT FIFO to the memory using the DMA peripheral by enabling 
    the CRYP_DMAReq_DataOUT request.

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the CRYP DMA interface.
  * @param  CRYP_DMAReq: specifies the CRYP DMA transfer request to be enabled or disabled.
  *           This parameter can be any combination of the following values:
  *            @arg CRYP_DMAReq_DataOUT: DMA for outgoing(Tx) data transfer
  *            @arg CRYP_DMAReq_DataIN: DMA for incoming(Rx) data transfer
  * @param  NewState: new state of the selected CRYP DMA transfer request.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void CRYP_DMACmd(uint8_t CRYP_DMAReq, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_CRYP_DMAREQ(CRYP_DMAReq));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Enable the selected CRYP DMA request */
    CRYP->DMACR |= CRYP_DMAReq;
  }
  else
  {
    /* Disable the selected CRYP DMA request */
    CRYP->DMACR &= (uint8_t)~CRYP_DMAReq;
  }
}
/**
  * @}
  */

/** @defgroup CRYP_Group5 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions
 *
@verbatim   
 ===============================================================================
                   Interrupts and flags management functions
 ===============================================================================  

  This section provides functions allowing to configure the CRYP Interrupts and 
  to get the status and Interrupts pending bits.

  The CRYP provides 2 Interrupts sources and 7 Flags:

  Flags :
  ------- 
                          
     1. CRYP_FLAG_IFEM :  Set when Input FIFO is empty.
                          This Flag is cleared only by hardware.
      
     2. CRYP_FLAG_IFNF :  Set when Input FIFO is not full.
                          This Flag is cleared only by hardware.


     3. CRYP_FLAG_INRIS  : Set when Input FIFO Raw interrupt is pending 
                           it gives the raw interrupt state prior to masking 
                           of the input FIFO service interrupt.
                           This Flag is cleared only by hardware.
     
     4. CRYP_FLAG_OFNE   : Set when Output FIFO not empty.
                           This Flag is cleared only by hardware.
        
     5. CRYP_FLAG_OFFU   : Set when Output FIFO is full.
                           This Flag is cleared only by hardware.
                           
     6. CRYP_FLAG_OUTRIS : Set when Output FIFO Raw interrupt is pending 
                           it gives the raw interrupt state prior to masking 
                           of the output FIFO service interrupt.
                           This Flag is cleared only by hardware.
                               
     7. CRYP_FLAG_BUSY   : Set when the CRYP core is currently processing a 
                           block of data or a key preparation (for AES 
                           decryption).
                           This Flag is cleared only by hardware.
                           To clear it, the CRYP core must be disabled and the 
                           last processing has completed. 

  Interrupts :
  ------------

   1. CRYP_IT_INI   : The input FIFO service interrupt is asserted when there 
                      are less than 4 words in the input FIFO.
                      This interrupt is associated to CRYP_FLAG_INRIS flag.

                @note This interrupt is cleared by performing write operations 
                      to the input FIFO until it holds 4 or more words. The 
                      input FIFO service interrupt INMIS is enabled with the 
                      CRYP enable bit. Consequently, when CRYP is disabled, the 
                      INMIS signal is low even if the input FIFO is empty.



   2. CRYP_IT_OUTI  : The output FIFO service interrupt is asserted when there 
                      is one or more (32-bit word) data items in the output FIFO.
                      This interrupt is associated to CRYP_FLAG_OUTRIS flag.

                @note This interrupt is cleared by reading data from the output 
                      FIFO until there is no valid (32-bit) word left (that is, 
                      the interrupt follows the state of the OFNE (output FIFO 
                      not empty) flag).


  Managing the CRYP controller events :
  ------------------------------------ 
  The user should identify which mode will be used in his application to manage 
  the CRYP controller events: Polling mode or Interrupt mode.

  1.  In the Polling Mode it is advised to use the following functions:
      - CRYP_GetFlagStatus() : to check if flags events occur. 

  @note  The CRYPT flags do not need to be cleared since they are cleared as 
         soon as the associated event are reset.   


  2.  In the Interrupt Mode it is advised to use the following functions:
      - CRYP_ITConfig()       : to enable or disable the interrupt source.
      - CRYP_GetITStatus()    : to check if Interrupt occurs.

  @note  The CRYPT interrupts have no pending bits, the interrupt is cleared as 
         soon as the associated event is reset. 

@endverbatim
  * @{
  */ 

/**
  * @brief  Enables or disables the specified CRYP interrupts.
  * @param  CRYP_IT: specifies the CRYP interrupt source to be enabled or disabled.
  *          This parameter can be any combination of the following values:
  *            @arg CRYP_IT_INI: Input FIFO interrupt
  *            @arg CRYP_IT_OUTI: Output FIFO interrupt
  * @param  NewState: new state of the specified CRYP interrupt.
  *           This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void CRYP_ITConfig(uint8_t CRYP_IT, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_CRYP_CONFIG_IT(CRYP_IT));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Enable the selected CRYP interrupt */
    CRYP->IMSCR |= CRYP_IT;
  }
  else
  {
    /* Disable the selected CRYP interrupt */
    CRYP->IMSCR &= (uint8_t)~CRYP_IT;
  }
}

/**
  * @brief  Checks whether the specified CRYP interrupt has occurred or not.
  * @note   This function checks the status of the masked interrupt (i.e the 
  *         interrupt should be previously enabled).     
  * @param  CRYP_IT: specifies the CRYP (masked) interrupt source to check.
  *           This parameter can be one of the following values:
  *            @arg CRYP_IT_INI: Input FIFO interrupt
  *            @arg CRYP_IT_OUTI: Output FIFO interrupt
  * @retval The new state of CRYP_IT (SET or RESET).
  */
ITStatus CRYP_GetITStatus(uint8_t CRYP_IT)
{
  ITStatus bitstatus = RESET;
  /* Check the parameters */
  assert_param(IS_CRYP_GET_IT(CRYP_IT));

  /* Check the status of the specified CRYP interrupt */
  if ((CRYP->MISR &  CRYP_IT) != (uint8_t)RESET)
  {
    /* CRYP_IT is set */
    bitstatus = SET;
  }
  else
  {
    /* CRYP_IT is reset */
    bitstatus = RESET;
  }
  /* Return the CRYP_IT status */
  return bitstatus;
}

/**
  * @brief  Checks whether the specified CRYP flag is set or not.
  * @param  CRYP_FLAG: specifies the CRYP flag to check.
  *          This parameter can be one of the following values:
  *            @arg CRYP_FLAG_IFEM: Input FIFO Empty flag.
  *            @arg CRYP_FLAG_IFNF: Input FIFO Not Full flag.
  *            @arg CRYP_FLAG_OFNE: Output FIFO Not Empty flag.
  *            @arg CRYP_FLAG_OFFU: Output FIFO Full flag.
  *            @arg CRYP_FLAG_BUSY: Busy flag.
  *            @arg CRYP_FLAG_OUTRIS: Output FIFO raw interrupt flag.
  *            @arg CRYP_FLAG_INRIS: Input FIFO raw interrupt flag.
  * @retval The new state of CRYP_FLAG (SET or RESET).
  */
FlagStatus CRYP_GetFlagStatus(uint8_t CRYP_FLAG)
{
  FlagStatus bitstatus = RESET;
  uint32_t tempreg = 0;

  /* Check the parameters */
  assert_param(IS_CRYP_GET_FLAG(CRYP_FLAG));

  /* check if the FLAG is in RISR register */
  if ((CRYP_FLAG & FLAG_MASK) != 0x00) 
  {
    tempreg = CRYP->RISR;
  }
  else  /* The FLAG is in SR register */
  {
    tempreg = CRYP->SR;
  }


  /* Check the status of the specified CRYP flag */
  if ((tempreg & CRYP_FLAG ) != (uint8_t)RESET)
  {
    /* CRYP_FLAG is set */
    bitstatus = SET;
  }
  else
  {
    /* CRYP_FLAG is reset */
    bitstatus = RESET;
  }

  /* Return the CRYP_FLAG status */
  return  bitstatus;
}

/**
  * @}
  */

/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
