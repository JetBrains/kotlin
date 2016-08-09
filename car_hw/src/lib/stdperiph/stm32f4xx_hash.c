/**
  ******************************************************************************
  * @file    stm32f4xx_hash.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the HASH / HMAC Processor (HASH) peripheral:           
  *           - Initialization and Configuration functions
  *           - Message Digest generation functions
  *           - context swapping functions   
  *           - DMA interface function       
  *           - Interrupts and flags management       
  *         
  *  @verbatim
  *                               
  *          ===================================================================      
  *                                   How to use this driver
  *          ===================================================================
  *          HASH operation : 
  *          ----------------                   
  *         1. Enable the HASH controller clock using 
  *            RCC_AHB2PeriphClockCmd(RCC_AHB2Periph_HASH, ENABLE) function.
  *           
  *         2. Initialise the HASH using HASH_Init() function. 
  *               
  *         3 . Reset the HASH processor core, so that the HASH will be ready 
  *             to compute he message digest of a new message by using 
  *             HASH_Reset() function.
  *
  *         4. Enable the HASH controller using the HASH_Cmd() function. 
  *                
  *         5. if using DMA for Data input transfer, Activate the DMA Request 
  *            using HASH_DMACmd() function 
  *                    
  *         6. if DMA is not used for data transfer, use HASH_DataIn() function 
  *            to enter data to IN FIFO.
  *             
  *          
  *         7. Configure the Number of valid bits in last word of the message 
  *            using HASH_SetLastWordValidBitsNbr() function.
  *             
  *         8. if the message length is not an exact multiple of 512 bits, 
  *            then the function HASH_StartDigest() must be called to 
  *            launch the computation of the final digest.     
  *             
  *         9. Once computed, the digest can be read using HASH_GetDigest() 
  *            function.         
  *                   
  *        10. To control HASH events you can use one of the following 
  *              two methods:
  *               a- Check on HASH flags using the HASH_GetFlagStatus() function.  
  *               b- Use HASH interrupts through the function HASH_ITConfig() at 
  *                  initialization phase and HASH_GetITStatus() function into 
  *                  interrupt routines in hashing phase.
  *          After checking on a flag you should clear it using HASH_ClearFlag()
  *          function. And after checking on an interrupt event you should 
  *          clear it using HASH_ClearITPendingBit() function.     
  *                     
  *        11. Save and restore hash processor context using 
  *            HASH_SaveContext() and HASH_RestoreContext() functions.     
  *              
  *
  *            
  *          HMAC operation : 
  *          ----------------  
  *          The HMAC algorithm is used for message authentication, by 
  *          irreversibly binding the message being processed to a key chosen 
  *          by the user. 
  *          For HMAC specifications, refer to "HMAC: keyed-hashing for message 
  *          authentication, H. Krawczyk, M. Bellare, R. Canetti, February 1997"
  *          
  *          Basically, the HMAC algorithm consists of two nested hash operations:
  *          HMAC(message) = Hash[((key | pad) XOR 0x5C) | Hash(((key | pad) XOR 0x36) | message)]
  *          where:
  *          - "pad" is a sequence of zeroes needed to extend the key to the 
  *                  length of the underlying hash function data block (that is 
  *                  512 bits for both the SHA-1 and MD5 hash algorithms)
  *          - "|"   represents the concatenation operator 
  *          
  *         
  *         To compute the HMAC, four different phases are required:
  *                    
  *         1.  Initialise the HASH using HASH_Init() function to do HMAC 
  *             operation. 
  *                
  *         2.  The key (to be used for the inner hash function) is then given 
  *             to the core. This operation follows the same mechanism as the 
  *             one used to send the message in the hash operation (that is, 
  *             by HASH_DataIn() function and, finally, 
  *             HASH_StartDigest() function.
  *          
  *         3.  Once the last word has been entered and computation has started, 
  *             the hash processor elaborates the key. It is then ready to 
  *             accept the message text using the same mechanism as the one 
  *             used to send the message in the hash operation.
  *       
  *         4.  After the first hash round, the hash processor returns "ready" 
  *             to indicate that it is ready to receive the key to be used for 
  *             the outer hash function (normally, this key is the same as the 
  *             one used for the inner hash function). When the last word of 
  *             the key is entered and computation starts, the HMAC result is 
  *             made available using HASH_GetDigest() function.
  *               
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
#include "stm32f4xx_hash.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup HASH 
  * @brief HASH driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/
/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/ 

/** @defgroup HASH_Private_Functions
  * @{
  */ 

/** @defgroup HASH_Group1 Initialization and Configuration functions
 *  @brief    Initialization and Configuration functions 
 *
@verbatim    
 ===============================================================================
                      Initialization and Configuration functions
 ===============================================================================  
  This section provides functions allowing to 
   - Initialize the HASH peripheral
   - Configure the HASH Processor 
      - MD5/SHA1, 
      - HASH/HMAC, 
      - datatype 
      - HMAC Key (if mode = HMAC)
   - Reset the HASH Processor 
   
@endverbatim
  * @{
  */
  
/**
  * @brief  Deinitializes the HASH peripheral registers to their default reset values
  * @param  None
  * @retval None
  */
void HASH_DeInit(void)
{
  /* Enable HASH reset state */
  RCC_AHB2PeriphResetCmd(RCC_AHB2Periph_HASH, ENABLE);
  /* Release HASH from reset state */
  RCC_AHB2PeriphResetCmd(RCC_AHB2Periph_HASH, DISABLE);
}

/**
  * @brief  Initializes the HASH peripheral according to the specified parameters
  *         in the HASH_InitStruct structure.
  * @note   the hash processor is reset when calling this function so that the
  *         HASH will be ready to compute the message digest of a new message.
  *         There is no need to call HASH_Reset() function.           
  * @param  HASH_InitStruct: pointer to a HASH_InitTypeDef structure that contains
  *         the configuration information for the HASH peripheral.
  * @note   The field HASH_HMACKeyType in HASH_InitTypeDef must be filled only 
  *          if the algorithm mode is HMAC.       
  * @retval None
  */
void HASH_Init(HASH_InitTypeDef* HASH_InitStruct)
{
  /* Check the parameters */
  assert_param(IS_HASH_ALGOSELECTION(HASH_InitStruct->HASH_AlgoSelection));
  assert_param(IS_HASH_DATATYPE(HASH_InitStruct->HASH_DataType));
  assert_param(IS_HASH_ALGOMODE(HASH_InitStruct->HASH_AlgoMode));
  
  /* Configure the Algorithm used, algorithm mode and the datatype */
  HASH->CR &= ~ (HASH_CR_ALGO | HASH_CR_DATATYPE | HASH_CR_MODE);
  HASH->CR |= (HASH_InitStruct->HASH_AlgoSelection | \
               HASH_InitStruct->HASH_DataType | \
               HASH_InitStruct->HASH_AlgoMode);
  
  /* if algorithm mode is HMAC, set the Key */  
  if(HASH_InitStruct->HASH_AlgoMode == HASH_AlgoMode_HMAC) 
  {
    assert_param(IS_HASH_HMAC_KEYTYPE(HASH_InitStruct->HASH_HMACKeyType));
    HASH->CR &= ~HASH_CR_LKEY;
    HASH->CR |= HASH_InitStruct->HASH_HMACKeyType;
  }

  /* Reset the HASH processor core, so that the HASH will be ready to compute 
     the message digest of a new message */
  HASH->CR |= HASH_CR_INIT;  
}

/**
  * @brief  Fills each HASH_InitStruct member with its default value.
  * @param  HASH_InitStruct : pointer to a HASH_InitTypeDef structure which will
  *          be initialized.  
  *  @note  The default values set are : Processor mode is HASH, Algorithm selected is SHA1,
  *          Data type selected is 32b and HMAC Key Type is short key.  
  * @retval None
  */
void HASH_StructInit(HASH_InitTypeDef* HASH_InitStruct)
{
  /* Initialize the HASH_AlgoSelection member */
  HASH_InitStruct->HASH_AlgoSelection = HASH_AlgoSelection_SHA1;

  /* Initialize the HASH_AlgoMode member */
  HASH_InitStruct->HASH_AlgoMode = HASH_AlgoMode_HASH;

  /* Initialize the HASH_DataType member */
  HASH_InitStruct->HASH_DataType = HASH_DataType_32b;

  /* Initialize the HASH_HMACKeyType member */
  HASH_InitStruct->HASH_HMACKeyType = HASH_HMACKeyType_ShortKey;
}

/**
  * @brief  Resets the HASH processor core, so that the HASH will be ready
  *         to compute the message digest of a new message.
  * @note   Calling this function will clear the HASH_SR_DCIS (Digest calculation 
  *         completion interrupt status) bit corresponding to HASH_IT_DCI 
  *         interrupt and HASH_FLAG_DCIS flag. 
  * @param  None
  * @retval None
  */
void HASH_Reset(void)
{
  /* Reset the HASH processor core */
  HASH->CR |= HASH_CR_INIT;
}
/**
  * @}
  */
 
/** @defgroup HASH_Group2 Message Digest generation functions
 *  @brief    Message Digest generation functions
 *
@verbatim    
 ===============================================================================
                      Message Digest generation functions
 ===============================================================================  
  This section provides functions allowing the generation of message digest: 
  - Push data in the IN FIFO : using HASH_DataIn()
  - Get the number of words set in IN FIFO, use HASH_GetInFIFOWordsNbr()  
  - set the last word valid bits number using HASH_SetLastWordValidBitsNbr() 
  - start digest calculation : using HASH_StartDigest()
  - Get the Digest message : using HASH_GetDigest()
 
@endverbatim
  * @{
  */


/**
  * @brief  Configure the Number of valid bits in last word of the message
  * @param  ValidNumber: Number of valid bits in last word of the message.
  *           This parameter must be a number between 0 and 0x1F.
  *             - 0x00: All 32 bits of the last data written are valid
  *             - 0x01: Only bit [0] of the last data written is valid
  *             - 0x02: Only bits[1:0] of the last data written are valid
  *             - 0x03: Only bits[2:0] of the last data written are valid
  *             - ...
  *             - 0x1F: Only bits[30:0] of the last data written are valid    
  * @note   The Number of valid bits must be set before to start the message 
  *         digest competition (in Hash and HMAC) and key treatment(in HMAC).    
  * @retval None
  */
void HASH_SetLastWordValidBitsNbr(uint16_t ValidNumber)
{
  /* Check the parameters */
  assert_param(IS_HASH_VALIDBITSNUMBER(ValidNumber));
  
  /* Configure the Number of valid bits in last word of the message */
  HASH->STR &= ~(HASH_STR_NBW);
  HASH->STR |= ValidNumber;
}

/**
  * @brief  Writes data in the Data Input FIFO
  * @param  Data: new data of the message to be processed.
  * @retval None
  */
void HASH_DataIn(uint32_t Data)
{
  /* Write in the DIN register a new data */
  HASH->DIN = Data;
}

/**
  * @brief  Returns the number of words already pushed into the IN FIFO.
  * @param  None
  * @retval The value of words already pushed into the IN FIFO.
  */
uint8_t HASH_GetInFIFOWordsNbr(void)
{
  /* Return the value of NBW bits */
  return ((HASH->CR & HASH_CR_NBW) >> 8);
}

/**
  * @brief  Provides the message digest result.
  * @note   In MD5 mode, Data[4] filed of HASH_MsgDigest structure is not used
  *         and is read as zero.  
  * @param  HASH_MessageDigest: pointer to a HASH_MsgDigest structure which will 
  *         hold the message digest result 
  * @retval None
  */
void HASH_GetDigest(HASH_MsgDigest* HASH_MessageDigest)
{
  /* Get the data field */
  HASH_MessageDigest->Data[0] = HASH->HR[0];
  HASH_MessageDigest->Data[1] = HASH->HR[1];
  HASH_MessageDigest->Data[2] = HASH->HR[2];
  HASH_MessageDigest->Data[3] = HASH->HR[3];
  HASH_MessageDigest->Data[4] = HASH->HR[4];
}

/**
  * @brief  Starts the message padding and calculation of the final message     
  * @param  None
  * @retval None
  */
void HASH_StartDigest(void)
{
  /* Start the Digest calculation */
  HASH->STR |= HASH_STR_DCAL;
}
/**
  * @}
  */

/** @defgroup HASH_Group3 Context swapping functions
 *  @brief   Context swapping functions
 *
@verbatim   
 ===============================================================================
                             Context swapping functions
 ===============================================================================  

  This section provides functions allowing to save and store HASH Context
  
  It is possible to interrupt a HASH/HMAC process to perform another processing 
  with a higher priority, and to complete the interrupted process later on, when 
  the higher priority task is complete. To do so, the context of the interrupted 
  task must be saved from the HASH registers to memory, and then be restored 
  from memory to the HASH registers.
  
  1. To save the current context, use HASH_SaveContext() function
  2. To restore the saved context, use HASH_RestoreContext() function 
  

@endverbatim
  * @{
  */
  
/**
  * @brief  Save the Hash peripheral Context. 
  * @note   The context can be saved only when no block is currently being 
  *         processed. So user must wait for DINIS = 1 (the last block has been 
  *         processed and the input FIFO is empty) or NBW != 0 (the FIFO is not 
  *         full and no processing is ongoing).   
  * @param  HASH_ContextSave: pointer to a HASH_Context structure that contains
  *         the repository for current context.
  * @retval None
  */
void HASH_SaveContext(HASH_Context* HASH_ContextSave)
{
  uint8_t i = 0;
  
  /* save context registers */
  HASH_ContextSave->HASH_IMR = HASH->IMR;  
  HASH_ContextSave->HASH_STR = HASH->STR;      
  HASH_ContextSave->HASH_CR  = HASH->CR;     
  for(i=0; i<=50;i++)
  {
     HASH_ContextSave->HASH_CSR[i] = HASH->CSR[i];
  }   
}

/**
  * @brief  Restore the Hash peripheral Context.  
  * @note   After calling this function, user can restart the processing from the
  *         point where it has been interrupted.  
  * @param  HASH_ContextRestore: pointer to a HASH_Context structure that contains
  *         the repository for saved context.
  * @retval None
  */
void HASH_RestoreContext(HASH_Context* HASH_ContextRestore)  
{
  uint8_t i = 0;
  
  /* restore context registers */
  HASH->IMR = HASH_ContextRestore->HASH_IMR;   
  HASH->STR = HASH_ContextRestore->HASH_STR;     
  HASH->CR = HASH_ContextRestore->HASH_CR;
  
  /* Initialize the hash processor */
  HASH->CR |= HASH_CR_INIT; 
  
   /* continue restoring context registers */     
  for(i=0; i<=50;i++)
  {
     HASH->CSR[i] = HASH_ContextRestore->HASH_CSR[i];
  }   
}
/**
  * @}
  */

/** @defgroup HASH_Group4 HASH's DMA interface Configuration function
 *  @brief   HASH's DMA interface Configuration function 
 *
@verbatim   
 ===============================================================================
                   HASH's DMA interface Configuration function
 ===============================================================================  

  This section provides functions allowing to configure the DMA interface for 
  HASH/ HMAC data input transfer.
   
  When the DMA mode is enabled (using the HASH_DMACmd() function), data can be 
  sent to the IN FIFO using the DMA peripheral.



@endverbatim
  * @{
  */
  
/**
  * @brief  Enables or disables the HASH DMA interface.
  * @note   The DMA is disabled by hardware after the end of transfer.
  * @param  NewState: new state of the selected HASH DMA transfer request.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void HASH_DMACmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Enable the HASH DMA request */
    HASH->CR |= HASH_CR_DMAE;
  }
  else
  {
    /* Disable the HASH DMA request */
    HASH->CR &= ~HASH_CR_DMAE;
  }
}
/**
  * @}
  */

/** @defgroup HASH_Group5 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions
 *
@verbatim   
 ===============================================================================
                   Interrupts and flags management functions
 ===============================================================================  

  This section provides functions allowing to configure the HASH Interrupts and 
  to get the status and clear flags and Interrupts pending bits.
  
  The HASH provides 2 Interrupts sources and 5 Flags:
  
  Flags :
  ---------- 
     1. HASH_FLAG_DINIS : set when 16 locations are free in the Data IN FIFO 
                          which means that a  new block (512 bit) can be entered 
                          into the input buffer.
                          
     2. HASH_FLAG_DCIS :  set when Digest calculation is complete
      
     3. HASH_FLAG_DMAS :  set when HASH's DMA interface is enabled (DMAE=1) or 
                          a transfer is ongoing.
                          This Flag is cleared only by hardware.
                           
     4. HASH_FLAG_BUSY :  set when The hash core is processing a block of data
                          This Flag is cleared only by hardware. 
                           
     5. HASH_FLAG_DINNE : set when Data IN FIFO is not empty which means that 
                          the Data IN FIFO contains at least one word of data.
                          This Flag is cleared only by hardware.
     
  Interrupts :
  ------------
    
   1. HASH_IT_DINI  : if enabled, this interrupt source is pending when 16 
                      locations are free in the Data IN FIFO  which means that 
                      a new block (512 bit) can be entered into the input buffer.
                      This interrupt source is cleared using 
                      HASH_ClearITPendingBit(HASH_IT_DINI) function.
   
   2. HASH_IT_DCI   : if enabled, this interrupt source is pending when Digest 
                      calculation is complete.
                      This interrupt source is cleared using 
                      HASH_ClearITPendingBit(HASH_IT_DCI) function.

  Managing the HASH controller events :
  ------------------------------------ 
  The user should identify which mode will be used in his application to manage 
  the HASH controller events: Polling mode or Interrupt mode.
  
  1.  In the Polling Mode it is advised to use the following functions:
      - HASH_GetFlagStatus() : to check if flags events occur. 
      - HASH_ClearFlag()     : to clear the flags events.
    
  2.  In the Interrupt Mode it is advised to use the following functions:
      - HASH_ITConfig()       : to enable or disable the interrupt source.
      - HASH_GetITStatus()    : to check if Interrupt occurs.
      - HASH_ClearITPendingBit() : to clear the Interrupt pending Bit 
                                (corresponding Flag). 

@endverbatim
  * @{
  */ 
  
/**
  * @brief  Enables or disables the specified HASH interrupts.
  * @param  HASH_IT: specifies the HASH interrupt source to be enabled or disabled.
  *          This parameter can be any combination of the following values:
  *            @arg HASH_IT_DINI: Data Input interrupt
  *            @arg HASH_IT_DCI: Digest Calculation Completion Interrupt
  * @param  NewState: new state of the specified HASH interrupt.
  *           This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void HASH_ITConfig(uint8_t HASH_IT, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_HASH_IT(HASH_IT));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Enable the selected HASH interrupt */
    HASH->IMR |= HASH_IT;
  }
  else
  {
    /* Disable the selected HASH interrupt */
    HASH->IMR &= (uint8_t) ~HASH_IT;
  }
}

/**
  * @brief  Checks whether the specified HASH flag is set or not.
  * @param  HASH_FLAG: specifies the HASH flag to check.
  *          This parameter can be one of the following values:
  *            @arg HASH_FLAG_DINIS: Data input interrupt status flag
  *            @arg HASH_FLAG_DCIS: Digest calculation completion interrupt status flag
  *            @arg HASH_FLAG_BUSY: Busy flag
  *            @arg HASH_FLAG_DMAS: DMAS Status flag
  *            @arg HASH_FLAG_DINNE: Data Input register (DIN) not empty status flag
  * @retval The new state of HASH_FLAG (SET or RESET)
  */
FlagStatus HASH_GetFlagStatus(uint16_t HASH_FLAG)
{
  FlagStatus bitstatus = RESET;
  uint32_t tempreg = 0;

  /* Check the parameters */
  assert_param(IS_HASH_GET_FLAG(HASH_FLAG));

  /* check if the FLAG is in CR register */
  if ((HASH_FLAG & HASH_FLAG_DINNE) != (uint16_t)RESET ) 
  {
    tempreg = HASH->CR;
  }
  else /* The FLAG is in SR register */
  {
    tempreg = HASH->SR;
  }

  /* Check the status of the specified HASH flag */
  if ((tempreg & HASH_FLAG) != (uint16_t)RESET)
  {
    /* HASH is set */
    bitstatus = SET;
  }
  else
  {
    /* HASH_FLAG is reset */
    bitstatus = RESET;
  }

  /* Return the HASH_FLAG status */
  return  bitstatus;
}
/**
  * @brief  Clears the HASH flags.
  * @param  HASH_FLAG: specifies the flag to clear. 
  *          This parameter can be any combination of the following values:
  *            @arg HASH_FLAG_DINIS: Data Input Flag
  *            @arg HASH_FLAG_DCIS: Digest Calculation Completion Flag                       
  * @retval None
  */
void HASH_ClearFlag(uint16_t HASH_FLAG)
{
  /* Check the parameters */
  assert_param(IS_HASH_CLEAR_FLAG(HASH_FLAG));
  
  /* Clear the selected HASH flags */
  HASH->SR = ~(uint32_t)HASH_FLAG;
}
/**
  * @brief  Checks whether the specified HASH interrupt has occurred or not.
  * @param  HASH_IT: specifies the HASH interrupt source to check.
  *          This parameter can be one of the following values:
  *            @arg HASH_IT_DINI: Data Input interrupt
  *            @arg HASH_IT_DCI: Digest Calculation Completion Interrupt
  * @retval The new state of HASH_IT (SET or RESET).
  */
ITStatus HASH_GetITStatus(uint8_t HASH_IT)
{
  ITStatus bitstatus = RESET;
  uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_HASH_GET_IT(HASH_IT));  


  /* Check the status of the specified HASH interrupt */
  tmpreg =  HASH->SR;

  if (((HASH->IMR & tmpreg) & HASH_IT) != RESET)
  {
    /* HASH_IT is set */
    bitstatus = SET;
  }
  else
  {
    /* HASH_IT is reset */
    bitstatus = RESET;
  }
  /* Return the HASH_IT status */
  return bitstatus;
}

/**
  * @brief  Clears the HASH interrupt pending bit(s).
  * @param  HASH_IT: specifies the HASH interrupt pending bit(s) to clear.
  *          This parameter can be any combination of the following values:
  *            @arg HASH_IT_DINI: Data Input interrupt
  *            @arg HASH_IT_DCI: Digest Calculation Completion Interrupt
  * @retval None
  */
void HASH_ClearITPendingBit(uint8_t HASH_IT)
{
  /* Check the parameters */
  assert_param(IS_HASH_IT(HASH_IT));

  /* Clear the selected HASH interrupt pending bit */
  HASH->SR = (uint8_t)~HASH_IT;
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
